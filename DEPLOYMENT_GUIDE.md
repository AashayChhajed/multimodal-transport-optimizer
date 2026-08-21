# Deployment Guide: Multimodal Transport Optimizer

## Overview
This guide walks through deploying the Multimodal Transport Optimizer application using:
- **NeonDB** for cloud PostgreSQL database
- **Render** for hosting backend (Java JAR) and frontend (Node.js)

---

## Prerequisites
- Git initialized and on `staging` branch
-- (Optional) Docker installed locally if you want to test container builds
- PostgreSQL client tools (`psql` or DBeaver) for database migration
- Accounts at:
  - [neon.tech](https://neon.tech) (NeonDB - free tier)
  - [render.com](https://render.com) (Render - free tier)
  - GitHub (for code hosting and Render integration)

---

## Phase 1: Database Migration to NeonDB

### Step 1.1: Create NeonDB Account and Project
1. Go to [neon.tech](https://neon.tech)
2. Sign up with email or GitHub account
3. Create a new project (default PostgreSQL version is fine)
4. In the project dashboard:
   - Note the **connection string** (looks like: `postgresql://user:password@host/dbname`)
   - Create a new branch/database if needed (default branch works)

### Step 1.2: Export Local Database
Open a terminal in the project root:

```bash
# Export your local PostgreSQL database schema and data
# Replace "optimizer_db" with your actual database name if different
pg_dump -U postgres -d optimizer_db -f db_backup.sql

# You may be prompted for the PostgreSQL password
```

**What this does:** Creates a SQL file with all your cities, routes, and transport mode data.

### Step 1.3: Import Data to NeonDB
```bash
# Get your NeonDB connection string from the Render dashboard
# It looks like: postgresql://user:password@host:5432/neon_db

# Import the backup to NeonDB
# Use the NeonDB connection details below:
psql "postgresql://your_neon_user:your_password@ep-xyz.neon.tech:5432/neon_db" -f db_backup.sql
```

**Replace with your actual NeonDB connection string** from the Neon dashboard.

### Step 1.4: Verify Data in NeonDB
```bash
# Connect to NeonDB and verify data
psql "postgresql://your_neon_user:your_password@ep-xyz.neon.tech:5432/neon_db"

# In psql prompt, run:
SELECT COUNT(*) FROM cities;
SELECT COUNT(*) FROM transport_modes;
SELECT COUNT(*) FROM routes;

# Exit psql
\q
```

You should see record counts matching your local database.

### Step 1.5: Test Connection Locally (Optional)
Update `backend/src/main/resources/application.properties` temporarily with a JDBC URL (include `jdbc:` prefix):

```properties
spring.datasource.url=jdbc:postgresql://ep-xyz.neon.tech:5432/neon_db?sslmode=require
spring.datasource.username=your_neon_user
spring.datasource.password=your_password
```

Run backend locally:
```bash
cd backend
mvn spring-boot:run
```

If it starts without database errors, your NeonDB connection works. Revert these changes afterward:
```bash
# Reset to localhost
git checkout backend/src/main/resources/application.properties
```

---

docker build -t transport-optimizer:latest .
## Phase 2: Build and Test the Backend (no Docker)

### Step 2.1: Build Locally with Maven
```bash
cd backend
mvn clean package -DskipTests
```

This builds a runnable JAR at `backend/target/backend-*.jar`.

### Step 2.2: Run Locally
```bash
$env:DB_URL = 'jdbc:postgresql://ep-xyz.neon.tech:5432/neon_db?sslmode=require'
$env:DB_USER = 'neon_user'
$env:DB_PASSWORD = 'neon_password'
$env:APP_SEED_ENABLED = 'true'  # only for initial seeding

cd backend
java -jar target/backend-*.jar
```

Test the API:
```bash
curl http://localhost:8080/cities
```

Press `Ctrl+C` to stop the process.

---

## Phase 3: Prepare Frontend Build

### Step 3.1: Verify Frontend Configuration
The frontend is already configured to use `NEXT_PUBLIC_BACKEND_URL` environment variable.

Check `frontend/lib/api.ts` line 48:
```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
```

This is already correct.

### Step 3.2: Build Frontend Locally
```bash
cd frontend

# Install dependencies (first time only)
npm install

# Build for production
npm run build
```

The build should complete without errors. Output goes to `.next/` directory.

---

## Phase 4: Deploy to Render

### Step 4.1: Prepare GitHub (or connect project repository)
1. Initialize git if not already done:
   ```bash
   cd e:\multimodal-transport-optimizer
   git init
   ```

2. Add and commit all changes on `staging` branch:
   ```bash
   git add .
   git commit -m "feat: add deployment configuration for NeonDB and Render"
   ```

3. Push to GitHub:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/multimodal-transport-optimizer.git
   git branch -M main  # rename current branch to main if needed
   git push -u origin staging
   ```

### Step 4.2: Deploy Backend to Render

1. Go to [render.com](https://render.com) and sign up/log in
2. Click **"New +"** → **"Web Service"**
3. Connect GitHub repository:
   - Choose your repo
   - Select `staging` branch (or main after merging)
4. Configure service:
   - **Name**: `transport-optimizer-backend`
   - **Root Directory**: `backend`
   - **Build Command**: `mvn clean package`
   - **Start Command**: `java -jar target/backend-*.jar`

5. Add Environment Variables (click **"Advanced"** → **"Add Environment Variable"**):
   - `DB_URL`: JDBC connection string for Spring (include the `jdbc:` prefix). Example:
     `jdbc:postgresql://ep-xyz.neon.tech:5432/neon_db?sslmode=require`
     (Use `?sslmode=require` if Neon requires SSL)
   - `DB_USER`: `neon_user`
   - `DB_PASSWORD`: `neon_password`
   - `APP_SEED_ENABLED`: `true` (set `true` only once for initial seeding, then set to `false`)
   - `CORS_ALLOWED_ORIGINS`: (set after frontend deployment)

6. Click **"Create Web Service"**
   - Render builds and deploys (takes 5-10 minutes first time)
   - Once live, you get a URL like: `https://transport-optimizer-backend.onrender.com`

7. **Note the backend URL** — you'll need it for frontend deployment

### Step 4.3: Test Backend Deployment
```bash
# Test the deployed API (replace with your actual Render URL)
curl https://transport-optimizer-backend.onrender.com/cities
```

Should return JSON. If 502 error, check Render logs for issues.

### Step 4.4: Deploy Frontend to Render

1. Click **"New +"** → **"Web Service"** on Render dashboard
2. Connect the same GitHub repository
3. Configure service:
   - **Name**: `transport-optimizer-frontend`
   - **Root Directory**: `frontend`
   - **Environment**: `Node`
   - **Build Command**: `npm install && npm run build`
   - **Start Command**: `npm start`

4. Add Environment Variables:
   - `NEXT_PUBLIC_BACKEND_URL`: `https://transport-optimizer-backend.onrender.com`
     (use the backend URL from Step 4.2)

5. Click **"Create Web Service"**
   - Render builds and deploys (takes 3-5 minutes)
   - Once live, you get a URL like: `https://transport-optimizer-frontend.onrender.com`

### Step 4.5: Update Backend CORS

Now that you have the frontend URL, update backend CORS:

1. Go back to backend service on Render
2. Click **"Environment"**
3. Update `CORS_ALLOWED_ORIGINS` to: `https://transport-optimizer-frontend.onrender.com`
4. Click **"Save"** — service auto-redeploys with new setting

---

## Phase 5: Post-Deployment Testing

### Step 5.1: Test Frontend Accessibility
1. Open `https://transport-optimizer-frontend.onrender.com` in browser
2. You should see the dashboard with shipment cards
3. Navigate to "Create Shipment"

### Step 5.2: Test End-to-End Flow
1. Create a new shipment:
   - Select source and destination cities
   - Set weight and description
   - Choose optimization goal (Cheapest/Fastest)
   - Click "Create & Optimize"

2. Verify:
   - No CORS errors in browser console
   - Shipment created successfully
   - Optimization results display

### Step 5.3: Verify Database Persistence
1. Create another shipment through the UI
2. Connect to NeonDB and verify:
   ```bash
   psql "postgresql://neon_user:neon_password@ep-xyz.neon.tech:5432/neon_db"
   
   # Count shipments
   SELECT COUNT(*) FROM shipments;
   ```

If count increased, data is persisting correctly.

### Step 5.4: Check Render Logs
- **Backend logs**: Render dashboard → transport-optimizer-backend → Logs
- **Frontend logs**: Render dashboard → transport-optimizer-frontend → Logs
- Look for any errors or warnings

---

## Common Issues & Troubleshooting

| Issue | Solution |
|-------|----------|
| **502 Bad Gateway** | Check Render logs; usually means backend crashed. Verify DB_URL env var. |
| **CORS errors in browser** | Ensure `CORS_ALLOWED_ORIGINS` on backend matches frontend URL exactly. |
| **"Cannot connect to database"** | Verify NeonDB connection string; test with `psql` first. |
| **Frontend shows 404** | Verify `NEXT_PUBLIC_BACKEND_URL` is correct; rebuild frontend. |
| **Services auto-sleep** | Normal on free tier; they wake on next request (30s startup). |

---

## Free Tier Monitoring

Check monthly:
1. **NeonDB Dashboard**: Storage usage (limit: 3 GB)
2. **Render Dashboard**: Bandwidth usage (limit: 100 GB/month)
3. **Email alerts**: Both services email you before limits

---

## Next Steps After Deployment

1. **Merge staging → main** when confident
   ```bash
   git checkout main
   git merge staging
   git push origin main
   ```

2. **Document your deployment** — write down:
   - NeonDB project ID and connection details (securely stored)
   - Render service URLs
   - Environment variables used
   - Any customizations made

3. **Optional enhancements**:
   - Add rate limiting if you notice excessive traffic
   - Set up Render email alerts for downtime
   - Monitor database growth (free tier has 3 GB limit)

---

## Cleanup

To delete your deployment (if learning is complete):
1. On Render, delete both services (backend and frontend)
2. On NeonDB, delete the project
3. Leave GitHub repo for reference

---

For questions, refer to:
- [Render Docs](https://render.com/docs)
- [NeonDB Docs](https://neon.tech/docs)
- [Docker Docs](https://docs.docker.com)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Next.js Docs](https://nextjs.org/docs)
