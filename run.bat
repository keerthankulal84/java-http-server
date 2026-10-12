@echo off
REM Compiles and runs the server using only the JDK (no Maven needed). Requires JDK 17+.
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
echo Starting server...
java -cp "out;src\main\resources" com.keerthan.httpserver.Main server.properties
