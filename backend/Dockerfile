# syntax=docker/dockerfile:1
# 后端镜像：Maven 构建 → JRE 21 运行（构建上下文为仓库根目录）

# ---------- 1. 构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
# 可选 Maven 镜像源：默认走 Maven Central，国内网络可由 compose 传入 MAVEN_MIRROR_URL
ARG MAVEN_MIRROR_URL=
WORKDIR /app
COPY backend/pom.xml ./backend/pom.xml
RUN if [ -n "$MAVEN_MIRROR_URL" ]; then \
      mkdir -p /root/.m2 && \
      printf '<settings><mirrors><mirror><id>mirror</id><mirrorOf>central</mirrorOf><url>%s</url></mirror></mirrors></settings>\n' "$MAVEN_MIRROR_URL" > /root/.m2/settings.xml; \
    fi
RUN mvn -q -f backend/pom.xml -DskipTests dependency:go-offline
COPY backend/src ./backend/src
RUN mvn -q -f backend/pom.xml -DskipTests package

# ---------- 2. 运行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/backend/target/campushub-backend-0.0.1-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    TZ=Asia/Shanghai
EXPOSE 8080
USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
