# Multithreaded HTTP Web Server in Core Java

An HTTP/1.1 web server built **from scratch using only the JDK** (no Spring, no Tomcat, no external libraries).
It demonstrates sockets, concurrency, collections, I/O, exception handling, and unit/integration testing.

## Features
- Accepts TCP connections with `java.net.ServerSocket`; each request runs on a fixed **thread pool** (`ExecutorService`)
- **Hand-written HTTP parser**: request line, headers, query string, URL decoding, `Content-Length` bodies
- Proper status codes: 200, 400, 403, 404, 405, 413, 429, 500, 505
- **Router** with lambda handlers (`router.get("/api/hello", req -> ...)`)
- **Static file server** with MIME types, path-traversal protection, and a thread-safe in-memory cache (`ConcurrentHashMap`)
- **Per-IP rate limiter** (fixed window) and lock-free request statistics (`AtomicLong`, `LongAdder`)
- JSON responses via a small custom JSON writer
- Request logging with `java.util.logging`, config from `server.properties`
- Graceful shutdown hook, socket read timeouts, request size limits
- JUnit 5 unit tests and an integration test that starts the real server on a random port

## Architecture

```
 Browser / curl
      |
      v
 +-----------+   accept()   +--------------------+
 | acceptor  | -----------> | worker thread pool |  (ExecutorService, N threads)
 |  thread   |   socket     +---------+----------+
 +-----------+                        |
                                      v
                              ClientHandler (Runnable)
                    parse -> rate limit -> route -> write response
                      |           |          |
                HttpRequest  RateLimiter   Router
                                              |-- ApiRoutes (/api/*)
                                              '-- StaticFileHandler (fallback, file cache)
```

## Project structure
```
src/main/java/com/keerthan/httpserver/
  Main.java              entry point, config + shutdown hook
  HttpServer.java        accept loop + thread pool
  ClientHandler.java     handles one connection
  ServerConfig.java      loads server.properties
  http/                  HttpRequest (parser), HttpResponse, Router, Handler, HttpException
  handlers/              ApiRoutes (REST endpoints), StaticFileHandler
  util/                  Json, RateLimiter, ServerStats
src/test/java/...        JUnit 5 tests
public/                  static website served at /
server.properties        port, threads, rate limit, timeouts
```

## Requirements
JDK 17 or newer (check with `java -version` and `javac -version`).

## Run (no Maven needed)
```bash
./run.sh          # Linux / macOS
run.bat           # Windows
```
Then open http://localhost:8080

## Run with Maven (tests + jar)
```bash
mvn test
mvn package
java -jar target/java-http-server-1.0.0.jar
```
Run the jar from the project folder so it finds `public/` and `server.properties`.

## API
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Static website (`public/index.html`) |
| GET | `/api/health` | `{"status":"UP"}` |
| GET | `/api/time` | Current UTC time |
| GET | `/api/hello?name=Ann` | `{"message":"Hello, Ann!"}` |
| GET | `/api/stats` | Uptime, total requests, active connections, counts per status code |
| POST | `/api/echo` | Echoes the body, content type, and the worker thread name |

```bash
curl "http://localhost:8080/api/hello?name=Keerthan"
curl -X POST -d "hello" http://localhost:8080/api/echo
curl http://localhost:8080/api/stats
```

## Configuration (`server.properties`)
| Key | Default | Meaning |
|-----|---------|---------|
| `server.port` | 8080 | Listening port (0 = random free port) |
| `server.threads` | 8 | Worker thread pool size |
| `server.publicDir` | public | Folder for static files |
| `server.rateLimitPerMinute` | 120 | Max requests per IP per minute (0 = off) |
| `server.readTimeoutMillis` | 5000 | Socket read timeout |

## Ideas to extend (good talking points)
- Keep-alive connections and chunked transfer encoding
- A JDBC-backed endpoint (e.g. `/api/notes` with H2 or MySQL)
- Virtual threads (Java 21): swap the fixed pool for `Executors.newVirtualThreadPerTaskExecutor()`
- NIO (`Selector`) based non-blocking version for comparison
- HTTPS with `SSLServerSocket`

## Resume entry
**Multithreaded HTTP Web Server – Core Java (JDK 17)**
Built an HTTP/1.1 server from scratch using Java sockets and a fixed thread pool to handle concurrent requests, with custom request parsing, routing, static file serving, and JSON responses. Implemented thread-safe caching with `ConcurrentHashMap`, per-IP rate limiting, request logging, and JUnit tests.
