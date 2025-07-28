FROM eclipse-temurin:17-jdk-jammy

# 1. Download driver FIRST to a temporary location
RUN mkdir -p /tmp/downloads && \
    apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    wget -O /tmp/downloads/postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar && \
    rm -rf /var/lib/apt/lists/*

# 2. Set working directory
WORKDIR /app

# 3. Copy files (including the downloaded driver)
COPY . .
COPY --from=0 /tmp/downloads/postgresql.jar .

# 4. Compile and run
RUN javac -cp .:postgresql.jar New.java
CMD ["java", "-cp", ".:postgresql.jar", "New"]