FROM python:3.11-slim

WORKDIR /app

# Install dependencies first for Docker layer caching
COPY service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy service code
COPY service/*.py service/

# Copy trained model
COPY models/ models/

EXPOSE 5000

# Set PYTHONPATH so imports from service/ work
ENV PYTHONPATH=/app/service

CMD ["python", "service/app.py"]
