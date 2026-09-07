@echo off
setlocal
cd /d %~dp0

set PORT=%1
if "%PORT%"=="" set PORT=8080

set M2=%USERPROFILE%\.m2\repository
set CP=%M2%\org\eclipse\jetty\jetty-http\11.0.20\jetty-http-11.0.20.jar;%M2%\org\eclipse\jetty\jetty-io\11.0.20\jetty-io-11.0.20.jar;%M2%\org\eclipse\jetty\jetty-security\11.0.20\jetty-security-11.0.20.jar;%M2%\org\eclipse\jetty\jetty-server\11.0.20\jetty-server-11.0.20.jar;%M2%\org\eclipse\jetty\jetty-servlet\11.0.20\jetty-servlet-11.0.20.jar;%M2%\org\eclipse\jetty\jetty-util\11.0.20\jetty-util-11.0.20.jar;%M2%\org\eclipse\jetty\toolchain\jetty-jakarta-servlet-api\5.0.2\jetty-jakarta-servlet-api-5.0.2.jar;%M2%\com\h2database\h2\2.2.224\h2-2.2.224.jar;%M2%\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar

echo ===================================================
echo  ConWork Attendance System - Starting (no Maven needed)
echo ===================================================

if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt

echo Compiling...
javac -encoding UTF-8 -cp "%CP%" -d out @sources.txt
if errorlevel 1 (
    echo.
    echo Build failed.
    pause
    exit /b 1
)

echo.
echo Starting server. Open http://localhost:%PORT%/ in your browser.
echo If that port is busy, run:  run.bat 8081
echo Press Ctrl+C in this window to stop.
echo.

java -cp "out;src\main\resources;%CP%" com.conwork.attendance.Main %PORT%

pause
