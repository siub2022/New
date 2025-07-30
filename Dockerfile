FROM openjdk:17
WORKDIR /app
COPY postgresql-42.7.3.jar .
COPY Music.java .
COPY start.sh .
RUN javac -cp ".:postgresql-42.7.3.jar" Music.java
RUN chmod +x start.sh
CMD ["./start.sh"]