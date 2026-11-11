#!/bin/sh
# Build Kaiju Kings -> build/libs/kaiju-kings-1.0.0.jar
# Needs: JDK 17 and Gradle 8.5+ (or run "gradle wrapper --gradle-version 8.5" once)
set -e
if [ -x ./gradlew ]; then ./gradlew build; else gradle build; fi
echo "DONE: build/libs/kaiju-kings-1.0.0.jar (put in mods/ with Fabric API 0.92.2+1.20.1)"
