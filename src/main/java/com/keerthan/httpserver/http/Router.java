package com.keerthan.httpserver.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Maps "METHOD /path" to a {@link Handler}; unmatched GET requests fall through to a fallback (static files). */
public final class Router {

    private final Map<String, Handler> routes = new HashMap<>();
    private final Set<String> knownPaths = new HashSet<>();
    private Handler fallback = req -> HttpResponse.error(404, "Not found: " + req.path());

    public Router get(String path, Handler handler) {
        return add("GET", path, handler);
    }

    public Router post(String path, Handler handler) {
        return add("POST", path, handler);
    }

    private Router add(String method, String path, Handler handler) {
        routes.put(method + " " + path, handler);
        knownPaths.add(path);
        return this;
    }

    /** Handler used for GET requests that match no registered route. */
    public Router fallback(Handler handler) {
        this.fallback = handler;
        return this;
    }

    public HttpResponse route(HttpRequest req) throws Exception {
        Handler handler = routes.get(req.method() + " " + req.path());
        if (handler != null) {
            return handler.handle(req);
        }
        if (knownPaths.contains(req.path())) {
            return HttpResponse.error(405, req.method() + " is not allowed on " + req.path());
        }
        if (req.method().equals("GET")) {
            return fallback.handle(req);
        }
        return HttpResponse.error(404, "Not found: " + req.path());
    }
}
