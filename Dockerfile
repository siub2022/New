FROM openjdk:17
WORKDIR /app
COPY . .
RUN javac -cp ".:postgresql-42.7.3.jar" Music.java  # Add this line
RUN chmod +x start.sh
CMD ["./start.sh"]