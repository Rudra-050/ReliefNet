# Use official Node.js LTS runtime
FROM node:20-slim

# Create app directory
WORKDIR /app

# Copy package files from server directory
COPY server/package*.json ./

# Install dependencies (production only)
RUN npm ci --only=production

# Copy app source from server directory
COPY server/ .

# Cloud Run sets PORT env var automatically (default 8080)
# Your server.js already uses process.env.PORT || 5000
EXPOSE 8080

# Start server
CMD ["node", "server.js"]
