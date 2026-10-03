# Convenience targets so the project always builds and runs with JDK 21,
# no matter which JDK happens to be first on PATH.
# Override with:  make run JAVA_HOME=/path/to/jdk
JAVA_HOME ?= /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME

.PHONY: java run test build clean

run: java
	./mvnw spring-boot:run

test: java
	./mvnw test

build: java
	./mvnw -DskipTests package

clean:
	./mvnw clean

java:
	@$(JAVA_HOME)/bin/java -version
