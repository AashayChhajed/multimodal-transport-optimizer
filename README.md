# Multi-Modal Transportation Cost & Delivery Optimizer

A full-stack logistics optimization platform that computes the most cost-effective, time-efficient, and eco-friendly delivery routes using multiple transportation modes (road, rail, air, sea).

Built with Next.js + Spring Boot + PostgreSQL + Python ML Service.

---

## Tech Stack

Frontend
- Next.js 16 (React 19)
- Tailwind CSS
- shadcn/ui

Backend
- Spring Boot 4.0.2 (Java 21)
- Spring Data JPA

Database
- PostgreSQL (NeonDB in production)

ML Service
- Python 3.11
- Flask
- XGBoost
- scikit-learn

---

## Architecture

```
Frontend (Next.js)
       ↓
Spring Boot Backend (Java)
       ↓
Python ML Service (Flask)
       ↓
XGBoost ETA Model
```

The backend remains the primary application server. The Python ML service is a lightweight sidecar responsible only for ETA predictions using a trained XGBoost model.

---

## Project Structure

```
multimodal-transport-optimizer/
├── backend/          # Spring Boot backend
│   ├── src/main/java/com/optimizer/backend/
│   │   ├── ml/       # ML integration (client, service, DTOs)
│   │   └── ...
│   └── Dockerfile
├── frontend/         # Next.js frontend
├── ml/               # Python ML pipeline & service
│   ├── service/      # Flask inference service
│   ├── models/       # Trained XGBoost model
│   ├── tests/        # Python test suite
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## Prerequisites

Make sure you have installed:
- Java 21
- Node.js 18+
- PostgreSQL
- Python 3.11+
- Git

Optional (for containerized deployment):
- Docker & Docker Compose

---

## ML Service

The ML service provides ETA (Estimated Time of Arrival) predictions using a trained XGBoost model.

### Model Details

- Model type: XGBoost Regressor
- Training data: Synthetic dataset (3,000 records)
- Features: 12 (7 numerical + 5 categorical)
- XGBoost test R² ≈ 0.959, Test MAE ≈ 1.86 hours (on synthetic evaluation data)

> **Important:** The model is trained on synthetic data. Its accuracy does NOT represent real-world ETA performance. The metrics above are results on synthetic evaluation data only.

### API Endpoints

**POST /predict-eta**

Request:
```json
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
```

Response:
```json
{
  "predicted_eta_hours": 13.91,
  "model": "XGBoost",
  "model_version": "1.0"
}
```

**GET /health**

Response:
```json
{
  "status": "UP",
  "model_loaded": true
}
```

### Model Features

Numerical: distance_km, shipment_weight_kg, departure_hour, day_of_week, month, transfer_count, historical_delay_rate

Categorical: source_city, destination_city, transport_mode, traffic_level, weather_condition

### Fallback Behavior

If the ML service is unavailable:
- Route optimization continues normally
- `predictedEtaHours` is null
- `etaPredictionAvailable` is false
- No fabricated fallback predictions are returned

---

## Local Development Setup

### Backend

```bash
cd backend
# Set environment variables (DB_URL, DB_USERNAME, DB_PASSWORD)
mvn spring-boot:run
```

Backend: http://localhost:8080

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:3000

### ML Service

```bash
cd ml
pip install -r requirements.txt
pip install flask
python service/app.py
```

ML Service: http://localhost:5000

### Run Python Tests

```bash
cd ml
python -m pytest tests/ -v
```

---

## Docker Setup

### Using Docker Compose

```bash
# Set required environment variables in .env
# Then run:
docker compose build
docker compose up
```

Services:
- Backend: http://localhost:8080
- ML Service: http://localhost:5000
- Frontend: http://localhost:3000 (run separately)

### Running Tests

```bash
# Backend tests
cd backend && mvn test

# Python tests
cd ml && python -m pytest tests/ -v

# Frontend build
cd frontend && npm run build
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|----------|
| DB_URL | PostgreSQL connection URL | - |
| DB_USERNAME | Database username | - |
| DB_PASSWORD | Database password | - |
| CORS_ALLOWED_ORIGINS | Allowed CORS origins | http://localhost:3000 |
| APP_SEED_ENABLED | Enable data seeding | true |
| NEXT_PUBLIC_API_URL | Backend API URL for frontend | http://localhost:8080 |
| ML_SERVICE_URL | Python ML service URL | http://localhost:5000 |
| ML_SERVICE_TIMEOUT | ML service timeout (ms) | 5000 |
| ML_SERVICE_PORT | ML service port | 5000 |

---

## Known Limitations

- The ML model is trained on synthetic data — real-world accuracy is unknown
- ML feature defaults: traffic_level=MEDIUM, weather_condition=CLEAR, historical_delay_rate=0.10 (no real-time data yet)
- City names in ML requests use IDs (feature mapping to city names not yet implemented)
- No real-time traffic or weather integration

---

## License

See repository for license details.
