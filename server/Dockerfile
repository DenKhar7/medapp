
# Stage 1: Cache Gradle dependencies
FROM gradle:9-jdk17 AS cache
RUN apt-get -y update && apt-get -y install curl # Utile pour le healthcheck
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle dependencies --no-daemon

# Stage 2: Build Application
FROM gradle:9-jdk17 AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM amazoncorretto:17 AS runtime
EXPOSE 4000
# Utilisateur non privilégié (uid 1001) + dossiers de travail des jobs d'import
RUN mkdir -p /app /var/server/data /var/server/temp && chown -R 1001:1001 /var/server
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-server-all.jar
USER 1001:1001
ENTRYPOINT ["java","-jar","/app/ktor-server-all.jar"]

