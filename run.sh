#!/usr/bin/env bash
# Compiles and runs the server using only the JDK (no Maven needed). Requires JDK 17+.
set -e
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
echo "Starting server..."
java -cp out:src/main/resources com.keerthan.httpserver.Main server.properties
