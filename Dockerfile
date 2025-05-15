FROM openjdk:11
//ARG JAR_FILE=2/target/rent-0.0.1-SNAPSHOT.jar
//COPY /target/rent-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone