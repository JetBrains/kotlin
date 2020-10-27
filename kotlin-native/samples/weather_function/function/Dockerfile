FROM openjdk:8u171-jdk-stretch as builder
WORKDIR /opt
ENV kotlin_native_ver "0.7.1"
ENV kotlin_native_home "/opt/kotlin-native-linux-$kotlin_native_ver"
RUN wget "https://github.com/JetBrains/kotlin-native/releases/download/v$kotlin_native_ver/kotlin-native-linux-$kotlin_native_ver.tar.gz" \
	&& tar -xzf kotlin-native-linux-$kotlin_native_ver.tar.gz && rm kotlin-native-linux-$kotlin_native_ver.tar.gz \
	&& apt-get -qy update && apt-get install libcurl4-openssl-dev \
	&& mkdir -p /app/src/main/kotlin/org/example/weather_func \
	&& mkdir -p /app/lib/cJSON-1.7.7/include/cjson && mkdir -p /app/lib/cJSON-1.7.7/lib/x86_64-linux-gnu \
	&& mkdir -p /app/gradle/wrapper
WORKDIR /app

COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar /app/gradle/wrapper/
COPY gradlew openweathermap_key.txt /app/
COPY .konan/ /root/.konan
COPY *.def *.kts /app/
COPY src/ /app/src
COPY lib/cJSON-1.7.7/include/cjson/cJSON.h /app/lib/cJSON-1.7.7/include/cJSON.h
COPY lib/cJSON-1.7.7/lib/x86_64-linux-gnu/libcjson.a /app/lib/cJSON-1.7.7/lib/libcjson.a
RUN ./gradlew build && cp build/konan/bin/linux_x64/weather.kexe weather

FROM debian:stretch
ADD https://github.com/openfaas/faas/releases/download/0.8.2/fwatchdog /usr/bin
RUN apt-get update && apt-get -qy install libcurl4-openssl-dev \
	&& chmod +x /usr/bin/fwatchdog && mkdir -p /app
WORKDIR /app
COPY --from=builder /app/openweathermap_key.txt .
COPY --from=builder /app/weather .
RUN chmod +x weather
ENV fprocess "./weather"
HEALTHCHECK --interval=2s CMD [ -e /tmp/.lock ] || exit 1
CMD ["fwatchdog"]
