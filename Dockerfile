# syntax=docker/dockerfile:1
# One image for the whole app: Maven builds the jar and (via frontend-maven-plugin) the React bundle.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
# Cache dependencies first: poms/lockfiles change rarely.
COPY backend/pom.xml backend/mvnw backend/
COPY backend/.mvn backend/.mvn
COPY api-client/package.json api-client/package-lock.json api-client/
COPY frontend/package.json frontend/package-lock.json frontend/
RUN cd backend && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY api-client api-client
COPY frontend frontend
COPY backend backend
RUN cd backend && chmod +x mvnw && ./mvnw -B -q -Pbundle-frontend -DskipTests package \
    && mv target/ihelper-*.jar /ihelper.jar

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 ihelper
USER ihelper
COPY --from=build /ihelper.jar /app/ihelper.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:MaxRAMPercentage=70"
ENTRYPOINT ["java", "-jar", "/app/ihelper.jar"]
