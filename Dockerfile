FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN javac New.java
CMD ["java", "New"]