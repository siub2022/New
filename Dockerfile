FROM eclipse-temurin:17-jdk-jammy

# 1. Install dependencies and clean up in one layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# 2. Set working directory
WORKDIR /app

# 3. Download driver with retries
RUN wget --tries=3 --waitretry=30 -O postgresql.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

# 4. Copy application files
COPY New.java .

# 5. Compile and run
RUN javac -cp .:postgresql.jar New.java
CMD ["java", "-cp", ".:postgresql.jar", "New"]