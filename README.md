# Multi-Modal Transportation Cost & Delivery Optimizer

A full-stack logistics optimization platform that computes the most cost-effective, time-efficient, and eco-friendly delivery routes using multiple transportation modes (road, rail, air, sea).

Built with Next.js + Spring Boot + PostgreSQL.

---

## Tech Stack

Frontend
- Next.js (React)
- Tailwind CSS
- shadcn/ui

Backend
- Spring Boot (Java 21)
- Spring Data JPA
- Spring Security
- Swagger (OpenAPI)

Database
- PostgreSQL

---

## 📂 Project Structure

```
multimodal-transport-optimizer/
├── backend/     # Spring Boot backend
├── frontend/    # Next.js frontend
├── docs/        # Architecture & design docs
└── README.md
```

---

## Prerequisites

Make sure you have installed:
- Java 21
- Node.js 18+
- PostgreSQL
- Git

---

## Backend Setup

1. Navigate to backend:
   cd backend

2. Configure database in ```src/main/resources/application.properties```

   Example:
   ```
   spring.datasource.url=jdbc:postgresql://localhost:5432/optimizer  
   spring.datasource.username=postgres  
   spring.datasource.password=yourpassword  

   spring.jpa.hibernate.ddl-auto=update  
   spring.jpa.show-sql=true
   ```

4. Run backend:
   ```mvn spring-boot:run```

5. Open Swagger:
   http://localhost:8080/swagger-ui.html

---

## Frontend Setup

1. Navigate to frontend:
   ```cd frontend```

2. Install dependencies:
   ```npm install```

3. Run frontend:
   ```npm run dev```

4. Open:
   http://localhost:3000

---

## Team Workflow

- Main branch: main
- Work on feature branches:
  - frontend
  - backend
  - feature/<name>

Use Pull Requests for merging.

---

## Project Goal

To build a real-world logistics optimization engine that:
- Calculates multiple delivery plans (cheapest, fastest, eco-friendly)
- Uses graph algorithms to optimize routes
- Visualizes transportation paths on a map
- Helps businesses reduce cost, delay, and carbon emissions

---

