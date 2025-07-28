FROM eclipse-temurin:17-jdk-jammy

# 1. Install dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# 2. Set up app directory
WORKDIR /app

# 3. Download PostgreSQL driver
RUN wget -O postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

# 4. Copy and compile
COPY Music.java .
RUN javac -cp .:postgresql.jar Music.java

CMD ["java", "-cp", ".:postgresql.jar", "Music"]