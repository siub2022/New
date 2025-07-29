# Use OpenJDK 17 as base image (ensures Java is installed)
FROM openjdk:17
# Set working directory
WORKDIR /app
# Copy all files (including start.sh and postgresql-42.7.3.jar)
COPY . .
# Make start.sh executable
RUN chmod +x start.sh
# Run your script (same as local)
CMD ["./start.sh"]