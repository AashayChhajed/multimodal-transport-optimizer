"""
ETA Prediction Service — Flask Application
============================================

A lightweight REST service that wraps the trained XGBoost ETA model
and exposes a simple HTTP API for ETA predictions.

Endpoints:
    POST /predict-eta  — Predict delivery ETA given shipment features
    GET  /health       — Service health and model availability check

Startup:
    The model is loaded ONCE at application startup. If the model
    file is missing or corrupted, the service fails immediately
    rather than serving degraded predictions.

Usage:
    python ml/service/app.py
    # or via Docker: see ml/service/Dockerfile
"""

import logging
import os
import sys

from flask import Flask, request, jsonify

from model_service import ModelService
from schemas import (
    parse_request,
    EtaPredictionResponse,
    HealthResponse,
)

# ============================================================
# LOGGING
# ============================================================

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("eta_service")

# ============================================================
# APPLICATION
# ============================================================

app = Flask(__name__)

# Global model service — initialized once at startup
model_service: ModelService = None  # type: ignore


def create_model_service() -> ModelService:
    """Create and load the model service. Exits on failure."""
    ms = ModelService()
    try:
        ms.load()
    except (FileNotFoundError, RuntimeError) as e:
        logger.error("FATAL: Could not load model: %s", e)
        sys.exit(1)
    return ms


# ============================================================
# ROUTES
# ============================================================


@app.route("/health", methods=["GET"])
def health():
    """
    Health check endpoint.

    Returns service status and whether the model is loaded.

    Response:
        {
            "status": "UP",
            "model_loaded": true
        }
    """
    resp = HealthResponse(
        status="UP",
        model_loaded=model_service.is_loaded if model_service else False,
    )
    return jsonify(resp.to_dict()), 200


@app.route("/predict-eta", methods=["POST"])
def predict_eta():
    """
    Predict delivery ETA for a shipment.

    Request body (JSON):
        {
            "distance_km": 500,
            "shipment_weight_kg": 500,
            "departure_hour": 10,
            "day_of_week": 2,
            "month": 8,
            "source_city": "Istanbul",
            "destination_city": "Ankara",
            "transport_mode": "ROAD",
            "traffic_level": "MEDIUM",
            "weather_condition": "CLEAR",
            "transfer_count": 1,
            "historical_delay_rate": 0.10
        }

    Success Response (200):
        {
            "predicted_eta_hours": 9.42,
            "model": "XGBoost",
            "model_version": "1.0"
        }

    Error Responses:
        400 — Invalid input (missing fields, bad values)
        500 — Prediction error
    """
    # 1. Parse request body
    try:
        data = request.get_json(force=True)
    except Exception:
        return jsonify({"error": "Request body must be valid JSON"}), 400

    if not data:
        return jsonify({"error": "Request body is empty"}), 400

    # 2. Parse into schema
    try:
        req = parse_request(data)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400

    # 3. Validate fields
    errors = req.validate()
    if errors:
        return jsonify({"error": "Validation failed", "details": errors}), 400

    # 4. Check model availability
    if not model_service or not model_service.is_loaded:
        return jsonify({"error": "Model is not available"}), 503

    # 5. Perform prediction
    try:
        prediction = model_service.predict({
            "distance_km": req.distance_km,
            "shipment_weight_kg": req.shipment_weight_kg,
            "departure_hour": req.departure_hour,
            "day_of_week": req.day_of_week,
            "month": req.month,
            "source_city": req.source_city,
            "destination_city": req.destination_city,
            "transport_mode": req.transport_mode,
            "traffic_level": req.traffic_level,
            "weather_condition": req.weather_condition,
            "transfer_count": req.transfer_count,
            "historical_delay_rate": req.historical_delay_rate,
        })

        response = EtaPredictionResponse(
            predicted_eta_hours=prediction,
            model=model_service.model_name,
            model_version=model_service.model_version,
        )
        return jsonify(response.to_dict()), 200

    except Exception as e:
        logger.error("Prediction failed: %s", e)
        return jsonify({"error": f"Prediction failed: {str(e)}"}), 500


# ============================================================
# STARTUP
# ============================================================

if __name__ == "__main__":
    port = int(os.environ.get("ML_SERVICE_PORT", 5000))
    debug = os.environ.get("ML_SERVICE_DEBUG", "false").lower() == "true"

    logger.info("Starting ETA Prediction Service on port %d ...", port)

    # Load model before starting the server
    model_service = create_model_service()
    logger.info("Model loaded. Starting Flask server ...")

    app.run(host="0.0.0.0", port=port, debug=debug)
