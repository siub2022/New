FROM eclipse-temurin:17-jdk-jammy

# 1. Download driver to root first (avoid permission issues)
RUN mkdir -p /drivers && \
    apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    wget -O /drivers/postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar && \
    rm -rf /var/lib/apt/lists/*

# 2. Set up app directory
WORKDIR /app

# 3. Copy driver and application
COPY --from=0 /drivers/postgresql.jar .
COPY Music.java .

# 4. Compile with explicit classpath
RUN javac -cp .:postgresql.jar Music.java

# 5. Force driver loading at runtime
CMD ["java", "-Djdbc.drivers=org.postgresql.Driver", "-cp", ".:postgresql.jar", "Music"]