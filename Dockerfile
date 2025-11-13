FROM eclipse-temurin:17-jdk
ARG JAR_FILE=2/target/rent-0.0.1-SNAPSHOT.jar
COPY /target/rent-0.0.1-SNAPSHOT.jar app.jar

# JVM 메모리 및 GC 설정 (메모리 부족 문제 해결을 위해 더 보수적으로 설정)
# ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx384m \
#   -XX:+UseG1GC \
#   -XX:MaxGCPauseMillis=200 \
#   -Xss256k \
#   -XX:ActiveProcessorCount=2 \
#   -XX:ParallelGCThreads=1 \
#   -XX:ConcGCThreads=1 \
#   -XX:CICompilerCount=2 \
#   -XX:MaxMetaspaceSize=128m \
#   -Djava.util.concurrent.ForkJoinPool.common.parallelism=1"

ENTRYPOINT ["java","-jar","/app.jar"]

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

