# === Build stage ===
FROM maven:3.9-amazoncorretto-21 AS backend
WORKDIR /backend

# Copy pom and Lombok config first to leverage Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean install -DskipTests

# Extract layered JAR contents
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# === Runtime stage ===
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

ARG DEPENDENCY=/backend/target/dependency

COPY --from=backend ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=backend ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=backend ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","-cp",".:lib/*","com.nazir.inventory.InventoryApplication"]