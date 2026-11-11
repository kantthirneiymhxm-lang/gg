@echo off
REM Build Kaiju Kings -> build\libs\kaiju-kings-1.0.0.jar
REM Needs: JDK 17 (java -version) and Gradle 8.5+ (gradle -v), or run "gradle wrapper --gradle-version 8.5" once.
if exist gradlew.bat (call gradlew.bat build) else (gradle build)
if errorlevel 1 (echo BUILD FAILED & pause & exit /b 1)
echo.
echo DONE: build\libs\kaiju-kings-1.0.0.jar  (copy it to .minecraft\mods together with Fabric API 0.92.2+1.20.1)
pause
