FROM eclipse-temurin:17-jdk-jammy

# 1. Install dependencies and download driver
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    wget -O /tmp/postgresql.jar https://jdbc.postgresql.org/download/postgresql-42.7.3.jar && \
    rm -rf /var/lib/apt/lists/*

# 2. Set up application directory
WORKDIR /app
COPY New.java .
RUN mv /tmp/postgresql.jar ./

# 3. Compile and run
RUN javac -cp .:postgresql.jar New.java
CMD ["java", "-cp", ".:postgresql.jar", "New"]