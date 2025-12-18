# Этап 1 — сборка на Java 25
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Копируем Gradle-файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Копируем код
COPY src src

# Даем права на исполнение и собираем
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# Этап 2 — финальный образ (JRE 25)
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Копируем готовый JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Не задаём фиксированный порт — Render передаст свой
EXPOSE 8080

# Порт jn Render (по умолчанию 8080)
# ENV PORT=8080
# EXPOSE ${PORT}

# Запуск с динамическим портом
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]



