FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

WORKDIR /app

COPY . /app

RUN mvn clean package -DskipTests

CMD ["java", "-jar", "target/stock-ml-0.0.1.jar"]