FROM eclipse-temurin:17-jdk-jammy

# 1. Install only what we need
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# 2. Set working directory
WORKDIR /app

# 3. Download driver and copy files in separate steps
RUN wget -O postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar
COPY New.java .

# 4. Compile and run
RUN javac -cp .:postgresql.jar New.java
CMD ["java", "-cp", ".:postgresql.jar", "New"]