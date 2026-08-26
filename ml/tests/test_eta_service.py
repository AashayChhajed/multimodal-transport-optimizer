"""
Tests for the ETA Prediction Service.

Covers:
1. Model loading
2. Valid prediction
3. Missing field handling
4. Invalid numerical values
5. Invalid categorical values
6. Health endpoint
7. Model unavailable behavior

All tests run locally without internet access.
"""

import json
import os
import sys
import pytest

# Ensure ml/service/ is importable
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "service"))

from model_service import ModelService
from schemas import parse_request, EtaPredictionRequest, VALID_TRANSPORT_MODES


# ============================================================
# FIXTURES
# ============================================================

MODEL_PATH = os.path.join(
    os.path.dirname(__file__), "..", "models", "eta_model.joblib"
)


@pytest.fixture(scope="module")
def model_service():
    """Load the model once for the entire test module."""
    ms = ModelService(model_path=MODEL_PATH)
    ms.load()
    return ms


@pytest.fixture(scope="module")
def flask_app():
    """Create a Flask test client with the model loaded."""
    from app import app, create_model_service

    app.config["TESTING"] = True
    # Replace the global model_service
    import app as app_module
    app_module.model_service = create_model_service()
    return app


@pytest.fixture(scope="module")
def client(flask_app):
    """Flask test client."""
    return flask_app.test_client()


VALID_REQUEST = {
    "distance_km": 500,
    "shipment_weight_kg": 500,
    "departure_hour": 10,
    "day_of_week": 2,
    "month": 8,
    "source_city": "Mumbai",
    "destination_city": "Delhi",
    "transport_mode": "ROAD",
    "traffic_level": "MEDIUM",
    "weather_condition": "CLEAR",
    "transfer_count": 1,
    "historical_delay_rate": 0.10,
}


# ============================================================
# 1. MODEL LOADING TESTS
# ============================================================

# All 20 application Indian cities
ALL_APPLICATION_CITIES = [
    "Mumbai", "Delhi", "Bengaluru", "Chennai", "Kolkata",
    "Hyderabad", "Pune", "Ahmedabad", "Jaipur", "Lucknow",
    "Surat", "Bhopal", "Indore", "Nagpur", "Patna",
    "Kochi", "Visakhapatnam", "Bhubaneswar", "Coimbatore", "Guwahati",
]


class TestModelLoading:
    def test_model_loads_successfully(self, model_service):
        """Model should load without errors."""
        assert model_service.is_loaded

    def test_model_has_correct_name(self, model_service):
        """Loaded model should be identified as XGBoost."""
        assert model_service.model_name == "XGBoost"

    def test_model_version_is_set(self, model_service):
        """Model version should be set after loading."""
        assert model_service.model_version is not None
        assert len(model_service.model_version) > 0


# ============================================================
# 2. VALID PREDICTION TESTS
# ============================================================

class TestValidPrediction:
    def test_prediction_returns_float(self, model_service):
        """Prediction should return a positive float."""
        result = model_service.predict(VALID_REQUEST)
        assert isinstance(result, float)
        assert result > 0

    def test_prediction_is_reasonable(self, model_service):
        """Prediction should be within a reasonable range for a 500km road trip."""
        result = model_service.predict(VALID_REQUEST)
        # 500km road at ~60km/h base ≈ 8.3h, with traffic/weather could be 10-20h
        assert 0.1 < result < 100, f"Prediction {result} seems unreasonable"

    def test_different_modes_give_different_predictions(self, model_service):
        """Different transport modes should yield different predictions."""
        road_result = model_service.predict(VALID_REQUEST)

        air_request = VALID_REQUEST.copy()
        air_request["transport_mode"] = "AIR"
        air_result = model_service.predict(air_request)

        assert road_result != air_result, "ROAD and AIR should produce different ETAs"

    def test_prediction_via_api(self, client):
        """POST /predict-eta should return a valid prediction."""
        response = client.post(
            "/predict-eta",
            data=json.dumps(VALID_REQUEST),
            content_type="application/json",
        )
        assert response.status_code == 200
        data = response.get_json()
        assert "predicted_eta_hours" in data
        assert "model" in data
        assert "model_version" in data
        assert data["model"] == "XGBoost"
        assert data["predicted_eta_hours"] > 0

    def test_mumbai_to_delhi_prediction(self, model_service):
        """Mumbai -> Delhi prediction should succeed."""
        request = VALID_REQUEST.copy()
        request["source_city"] = "Mumbai"
        request["destination_city"] = "Delhi"
        result = model_service.predict(request)
        assert result > 0, f"Mumbai->Delhi prediction should be positive, got {result}"

    def test_delhi_to_bengaluru_prediction(self, model_service):
        """Delhi -> Bengaluru prediction should succeed."""
        request = VALID_REQUEST.copy()
        request["source_city"] = "Delhi"
        request["destination_city"] = "Bengaluru"
        result = model_service.predict(request)
        assert result > 0, f"Delhi->Bengaluru prediction should be positive, got {result}"

    def test_all_application_cities_accepted_as_source(self, model_service):
        """Every application city should be accepted as source_city."""
        for city in ALL_APPLICATION_CITIES:
            request = VALID_REQUEST.copy()
            request["source_city"] = city
            request["destination_city"] = "Delhi" if city != "Delhi" else "Mumbai"
            result = model_service.predict(request)
            assert result > 0, f"City {city} as source should produce valid prediction"

    def test_all_application_cities_accepted_as_destination(self, model_service):
        """Every application city should be accepted as destination_city."""
        for city in ALL_APPLICATION_CITIES:
            request = VALID_REQUEST.copy()
            request["source_city"] = "Mumbai" if city != "Mumbai" else "Delhi"
            request["destination_city"] = city
            result = model_service.predict(request)
            assert result > 0, f"City {city} as destination should produce valid prediction"


# ============================================================
# 3. MISSING FIELD TESTS
# ============================================================

class TestMissingFields:
    def test_empty_body_returns_400(self, client):
        response = client.post(
            "/predict-eta",
            data="",
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_missing_distance_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        del payload["distance_km"]
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_missing_transport_mode_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        del payload["transport_mode"]
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_missing_multiple_fields_returns_400(self, client):
        payload = {"distance_km": 500}
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400
        data = response.get_json()
        assert "details" in data or "error" in data

    def test_invalid_json_returns_400(self, client):
        response = client.post(
            "/predict-eta",
            data="not json",
            content_type="application/json",
        )
        assert response.status_code == 400


# ============================================================
# 4. INVALID NUMERICAL VALUES TESTS
# ============================================================

class TestInvalidNumericalValues:
    def test_negative_distance_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["distance_km"] = -100
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400
        data = response.get_json()
        assert "distance_km" in str(data)

    def test_negative_weight_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["shipment_weight_kg"] = -50
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_invalid_hour_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["departure_hour"] = 25
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_invalid_day_of_week_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["day_of_week"] = 8
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_invalid_month_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["month"] = 13
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_negative_transfer_count_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["transfer_count"] = -1
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_delay_rate_out_of_range_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["historical_delay_rate"] = 1.5
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_schema_validation_rejects_negative(self):
        """Schema-level validation for negative distance."""
        req = EtaPredictionRequest(
            distance_km=-10, shipment_weight_kg=100,
            departure_hour=10, day_of_week=2, month=8,
            source_city="Mumbai", destination_city="Delhi",
            transport_mode="ROAD", traffic_level="LOW",
            weather_condition="CLEAR", transfer_count=0,
            historical_delay_rate=0.05,
        )
        errors = req.validate()
        assert any("distance_km" in e for e in errors)


# ============================================================
# 5. INVALID CATEGORICAL VALUES TESTS
# ============================================================

class TestInvalidCategoricalValues:
    def test_invalid_transport_mode_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["transport_mode"] = "BOAT"
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400
        data = response.get_json()
        assert "transport_mode" in str(data)

    def test_invalid_traffic_level_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["traffic_level"] = "EXTREME"
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_invalid_weather_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["weather_condition"] = "HAIL"
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400

    def test_empty_source_city_returns_400(self, client):
        payload = VALID_REQUEST.copy()
        payload["source_city"] = ""
        response = client.post(
            "/predict-eta",
            data=json.dumps(payload),
            content_type="application/json",
        )
        assert response.status_code == 400


# ============================================================
# 6. HEALTH ENDPOINT TESTS
# ============================================================

class TestHealthEndpoint:
    def test_health_returns_200(self, client):
        response = client.get("/health")
        assert response.status_code == 200

    def test_health_returns_correct_format(self, client):
        response = client.get("/health")
        data = response.get_json()
        assert data["status"] == "UP"
        assert "model_loaded" in data

    def test_health_reports_model_loaded(self, client):
        response = client.get("/health")
        data = response.get_json()
        assert data["model_loaded"] is True


# ============================================================
# 7. MODEL UNAVAILABLE BEHAVIOR TESTS
# ============================================================

class TestModelUnavailable:
    def test_prediction_fails_when_model_not_loaded(self):
        """ModelService without loading should not predict."""
        ms = ModelService(model_path="/nonexistent/path.joblib")
        assert not ms.is_loaded
        with pytest.raises(RuntimeError, match="Model is not loaded"):
            ms.predict(VALID_REQUEST)

    def test_load_nonexistent_model_raises_error(self):
        """Loading a nonexistent model file should raise FileNotFoundError."""
        ms = ModelService(model_path="/nonexistent/path.joblib")
        with pytest.raises(FileNotFoundError):
            ms.load()
