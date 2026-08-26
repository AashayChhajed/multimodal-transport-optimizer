"""
Request and response schemas for the ETA prediction API.

These schemas validate incoming requests and structure outgoing responses
for the /predict-eta endpoint.
"""

from dataclasses import dataclass, field
from typing import Optional


VALID_TRANSPORT_MODES = {"ROAD", "RAIL", "AIR"}
VALID_TRAFFIC_LEVELS = {"LOW", "MEDIUM", "HIGH"}
VALID_WEATHER_CONDITIONS = {"CLEAR", "RAIN", "SNOW", "STORM"}


@dataclass
class EtaPredictionRequest:
    """Request schema for ETA prediction."""

    distance_km: float
    shipment_weight_kg: float
    departure_hour: int
    day_of_week: int
    month: int
    source_city: str
    destination_city: str
    transport_mode: str
    traffic_level: str
    weather_condition: str
    transfer_count: int
    historical_delay_rate: float

    def validate(self) -> list[str]:
        """Validate all fields. Returns a list of error messages (empty if valid)."""
        errors = []

        # Numerical validations
        if self.distance_km < 0:
            errors.append("distance_km must be non-negative")
        if self.shipment_weight_kg < 0:
            errors.append("shipment_weight_kg must be non-negative")
        if not (0 <= self.departure_hour <= 23):
            errors.append("departure_hour must be between 0 and 23")
        if not (0 <= self.day_of_week <= 6):
            errors.append("day_of_week must be between 0 and 6")
        if not (1 <= self.month <= 12):
            errors.append("month must be between 1 and 12")
        if self.transfer_count < 0:
            errors.append("transfer_count must be non-negative")
        if not (0.0 <= self.historical_delay_rate <= 1.0):
            errors.append("historical_delay_rate must be between 0.0 and 1.0")

        # Categorical validations
        if not self.source_city or not self.source_city.strip():
            errors.append("source_city is required and cannot be empty")
        if not self.destination_city or not self.destination_city.strip():
            errors.append("destination_city is required and cannot be empty")
        if self.transport_mode not in VALID_TRANSPORT_MODES:
            errors.append(
                f"transport_mode must be one of {VALID_TRANSPORT_MODES}, "
                f"got '{self.transport_mode}'"
            )
        if self.traffic_level not in VALID_TRAFFIC_LEVELS:
            errors.append(
                f"traffic_level must be one of {VALID_TRAFFIC_LEVELS}, "
                f"got '{self.traffic_level}'"
            )
        if self.weather_condition not in VALID_WEATHER_CONDITIONS:
            errors.append(
                f"weather_condition must be one of {VALID_WEATHER_CONDITIONS}, "
                f"got '{self.weather_condition}'"
            )

        return errors


@dataclass
class EtaPredictionResponse:
    """Response schema for ETA prediction."""

    predicted_eta_hours: float
    model: str
    model_version: str

    def to_dict(self) -> dict:
        return {
            "predicted_eta_hours": round(self.predicted_eta_hours, 2),
            "model": self.model,
            "model_version": self.model_version,
        }


@dataclass
class HealthResponse:
    """Response schema for health check."""

    status: str
    model_loaded: bool

    def to_dict(self) -> dict:
        return {
            "status": self.status,
            "model_loaded": self.model_loaded,
        }


def parse_request(data: dict) -> EtaPredictionRequest:
    """Parse a dictionary into an EtaPredictionRequest. Raises ValueError on missing fields."""
    required_fields = [
        "distance_km", "shipment_weight_kg", "departure_hour",
        "day_of_week", "month", "source_city", "destination_city",
        "transport_mode", "traffic_level", "weather_condition",
        "transfer_count", "historical_delay_rate",
    ]

    missing = [f for f in required_fields if f not in data]
    if missing:
        raise ValueError(f"Missing required fields: {', '.join(missing)}")

    return EtaPredictionRequest(
        distance_km=float(data["distance_km"]),
        shipment_weight_kg=float(data["shipment_weight_kg"]),
        departure_hour=int(data["departure_hour"]),
        day_of_week=int(data["day_of_week"]),
        month=int(data["month"]),
        source_city=str(data["source_city"]),
        destination_city=str(data["destination_city"]),
        transport_mode=str(data["transport_mode"]),
        traffic_level=str(data["traffic_level"]),
        weather_condition=str(data["weather_condition"]),
        transfer_count=int(data["transfer_count"]),
        historical_delay_rate=float(data["historical_delay_rate"]),
    )
