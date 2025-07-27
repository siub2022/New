FROM openjdk:17
COPY New.java .
RUN javac New.java
CMD ["java", "New"]