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

## Project Structure

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

## Branching Strategy

### Branches

- **main**
  - Stable branch
  - Contains production-ready and reviewed code only

- **dev**
  - Integration branch
  - All features are merged here before going to `main`

- **frontend**
  - Used for frontend (Next.js, UI, shadcn) development
  - Merged into `dev` via Pull Requests

- **backend**
  - Used for backend (Spring Boot, APIs, database) development
  - Merged into `dev` via Pull Requests

### Workflow

1. Developers work on either `frontend` or `backend` branch
2. Changes are pushed to the respective branch
3. A Pull Request is opened to merge into `dev`
4. After testing and review, `dev` is merged into `main`

This workflow helps keep the `main` branch stable while allowing parallel development.

---

## Project Goal

To build a real-world logistics optimization engine that:
- Calculates multiple delivery plans (cheapest, fastest, eco-friendly)
- Uses graph algorithms to optimize routes
- Visualizes transportation paths on a map
- Helps businesses reduce cost, delay, and carbon emissions

---

