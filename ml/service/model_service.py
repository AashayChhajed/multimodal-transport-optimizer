"""
Model service responsible for loading the trained XGBoost pipeline
and performing ETA predictions.

The model is loaded ONCE at startup. Subsequent requests use the
loaded pipeline for inference without reloading from disk.
"""

import os
import logging
from typing import Optional

import joblib
import pandas as pd

logger = logging.getLogger(__name__)


class ModelService:
    """
    Manages the lifecycle of the trained ML model.

    Responsibilities:
    - Load the serialized pipeline from disk at startup
    - Validate model availability
    - Perform predictions using the loaded pipeline
    """

    def __init__(self, model_path: Optional[str] = None):
        """
        Initialize the model service.

        Args:
            model_path: Path to the serialized model file.
                       If None, uses the default path relative to this file.
        """
        if model_path is None:
            model_path = os.path.join(
                os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                "models",
                "eta_model.joblib",
            )

        self._model_path = model_path
        self._pipeline = None
        self._model_name = None
        self._model_version = "1.0"
        self._feature_columns = None

    def load(self) -> None:
        """
        Load the model from disk. Called once at application startup.

        Raises:
            FileNotFoundError: If the model file does not exist.
            RuntimeError: If the model file is corrupted or cannot be loaded.
        """
        if not os.path.exists(self._model_path):
            raise FileNotFoundError(
                f"Model file not found: {self._model_path}"
            )

        try:
            logger.info("Loading model from %s ...", self._model_path)
            model_data = joblib.load(self._model_path)

            self._pipeline = model_data["pipeline"]
            self._model_name = model_data.get("model_name", "XGBoost")
            self._feature_columns = model_data.get("feature_columns", [])
            self._model_version = "1.0"

            logger.info(
                "Model loaded successfully: %s (version %s)",
                self._model_name,
                self._model_version,
            )
        except Exception as e:
            raise RuntimeError(f"Failed to load model from {self._model_path}: {e}") from e

    @property
    def is_loaded(self) -> bool:
        """Check if the model is loaded and ready for predictions."""
        return self._pipeline is not None

    @property
    def model_name(self) -> str:
        return self._model_name or "Unknown"

    @property
    def model_version(self) -> str:
        return self._model_version

    def predict(self, data: dict) -> float:
        """
        Perform an ETA prediction.

        Args:
            data: Dictionary with feature values matching the training schema.

        Returns:
            Predicted ETA in hours.

        Raises:
            RuntimeError: If the model is not loaded.
            ValueError: If the input data is invalid.
        """
        if not self.is_loaded:
            raise RuntimeError("Model is not loaded. Cannot perform prediction.")

        # Build a DataFrame with the expected feature columns
        # Feature order must match the training data exactly
        expected_features = [
            "source_city", "destination_city", "transport_mode", "distance_km",
            "shipment_weight_kg", "traffic_level", "weather_condition",
            "departure_hour", "day_of_week", "month", "transfer_count",
            "historical_delay_rate",
        ]

        # Verify all required features are present
        missing = [f for f in expected_features if f not in data]
        if missing:
            raise ValueError(f"Missing features for prediction: {', '.join(missing)}")

        # Create DataFrame in the exact column order
        input_data = pd.DataFrame([{col: data[col] for col in expected_features}])

        try:
            prediction = self._pipeline.predict(input_data)
            return float(prediction[0])
        except Exception as e:
            raise RuntimeError(f"Prediction failed: {e}") from e
