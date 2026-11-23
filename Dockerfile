FROM node:24-alpine AS css
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY src/main ./src/main
RUN npm run build:css

FROM eclipse-temurin:21-jdk-noble AS war
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src ./src
COPY --from=css /app/src/main/webapp/assets/app.css src/main/webapp/assets/app.css
RUN ./mvnw -q -B package -DskipTests

FROM tomcat:11.0.29-jre21-temurin-noble
RUN rm -rf /usr/local/tomcat/webapps/* \
    && useradd --system --uid 10001 --home-dir /usr/local/tomcat tracker \
    && mkdir /data \
    && chown -R tracker /usr/local/tomcat /data
COPY --from=war --chown=tracker /app/target/spending-tracker.war /usr/local/tomcat/webapps/ROOT.war
ENV DB_PATH=/data/tracker.db TZ=Asia/Jakarta
USER tracker
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD curl -fsS http://localhost:8080/up || exit 1
