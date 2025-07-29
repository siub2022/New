# Lightweight Alpine base (no Java - we override it!)
FROM alpine:latest
WORKDIR /app
COPY . .
# Make start.sh executable and RUN IT
RUN chmod +x start.sh
CMD ["./start.sh"]  # ← Overrides Docker to use your script