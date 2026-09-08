# Q-ify Frontend

This Angular application is the Week 1 browser-to-backend risk probe.

## Requirements

- Node.js 24.20.0
- npm 11.19.0
- The backend listening on `http://localhost:8080` for local development

## Commands

Install the exact locked dependencies:

```bash
npm ci
```

Start the development server with `/api` proxied to the backend:

```bash
npm start
```

Run the unit tests once:

```bash
npm test
```

Create the production build:

```bash
npm run build
```

The deep probe route is `http://localhost:4200/status/probe`.
