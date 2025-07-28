FROM eclipse-temurin:17-jdk

# 1. Install wget to download the driver
RUN apt-get update && apt-get install -y wget

# 2. Download PostgreSQL JDBC driver
RUN wget -O /app/postgresql.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

# 3. Set classpath
ENV CLASSPATH=/app/postgresql.jar:.

WORKDIR /app
COPY . .
RUN javac New.java
CMD ["java", "New"]