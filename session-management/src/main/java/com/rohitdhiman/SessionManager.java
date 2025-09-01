package com.rohitdhiman;

import spark.Spark;
import spark.Request;
import spark.Response;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.math.BigInteger;

public class SessionManager {

    private static JedisPool jedisPool;

    public static void main(String[] args) {
        // Configure Spark to use a fixed thread pool.
        Spark.threadPool(20);
        Spark.port(4567);
        // Serve static files from resources/public
        Spark.staticFiles.location("/public");

        // Get Redis host from environment variable
        String redisHost = System.getenv("REDIS_HOST");
        // Get Redis port from environment variable, default to 6379
        String redisPortStr = System.getenv("REDIS_PORT");
        int redisPort = 6379; // Default Redis port
        if (redisPortStr != null) {
            try {
                redisPort = Integer.parseInt(redisPortStr);
            } catch (NumberFormatException e) {
                // Use default if parsing fails
                System.err.println("Invalid REDIS_PORT environment variable. Using default port 6379.");
            }
        }

        System.out.println("Connecting to Redis at: " + redisHost + ":" + redisPort);
        jedisPool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
        System.out.println("Connected to Redis!");

        Spark.get("/", (req, res) -> {
            res.redirect("/login.html");
            return null;
        });

        // Helper to generate CSRF token
        SecureRandom random = new SecureRandom();
        java.util.function.Supplier<String> generateCsrfToken = () -> new BigInteger(130, random).toString(32);

        // Route to serve login.html with CSRF token injected
        Spark.get("/login.html", (req, res) -> {
            String csrfToken = generateCsrfToken.get();
            String sessionId = req.cookie("sessionId");
            if (sessionId != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.setex("csrf:" + sessionId, TimeUnit.MINUTES.toSeconds(15), csrfToken);
                }
            }
            java.io.InputStream in = SessionManager.class.getResourceAsStream("/public/login.html");
            if (in == null) throw new Exception("login.html not found in resources");
            String html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            html = html.replace("<form method=\"post\" action=\"/login\">", "<form method=\"post\" action=\"/login\"><input type=\"hidden\" name=\"csrfToken\" value=\"" + csrfToken + "\">");
            res.type("text/html");
            return html;
        });

        // Route to handle login form submission
        // Get credentials from environment variables, fallback to defaults
        final String ENV_USERNAME = System.getenv().getOrDefault("APP_USERNAME", "user");
        final String ENV_PASSWORD = System.getenv().getOrDefault("APP_PASSWORD", "password");

        Spark.post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String csrfToken = req.queryParams("csrfToken");
            String sessionId = req.cookie("sessionId");
            boolean csrfValid = false;
            if (sessionId != null && csrfToken != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    String storedToken = jedis.get("csrf:" + sessionId);
                    csrfValid = csrfToken.equals(storedToken);
                }
            }
            if (!csrfValid) {
                res.status(403);
                return "CSRF validation failed.";
            }
            if (ENV_USERNAME.equals(username) && ENV_PASSWORD.equals(password)) {
                String sessionIdNew = UUID.randomUUID().toString();
                String redisKey = "session:" + sessionIdNew;
                String csrfTokenNew = generateCsrfToken.get();
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.setex(redisKey, TimeUnit.MINUTES.toSeconds(15), username);
                    jedis.setex("csrf:" + sessionIdNew, TimeUnit.MINUTES.toSeconds(15), csrfTokenNew);
                }
                // Set secure, httpOnly cookie
                res.cookie("/", "sessionId", sessionIdNew, 15 * 60, true, true);
                res.redirect("/dashboard");
                return null;
            } else {
                return "Invalid credentials. <a href='/login'>Try again</a>.";
            }
        });

        // Protected dashboard route (static HTML, with username injected)
        Spark.get("/dashboard", (req, res) -> {
            String sessionId = req.cookie("sessionId");
            if (sessionId == null) {
                res.redirect("/login");
                return null;
            }

            String username;
            try (Jedis jedis = jedisPool.getResource()) {
                username = jedis.get("session:" + sessionId);
            }
            if (username == null) {
                res.removeCookie("sessionId");
                res.redirect("/login");
                return null;
            }

            res.type("text/html");
            try {
                java.io.InputStream in = SessionManager.class.getResourceAsStream("/public/dashboard.html");
                if (in == null) throw new Exception("dashboard.html not found in resources");
                String html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                // Simple username injection
                html = html.replace("Welcome!", "Welcome, " + username + "!");
                return html;
            } catch (Exception e) {
                res.status(500);
                return "Error loading dashboard page.";
            }
        });

        // Logout route
        Spark.get("/logout", (req, res) -> {
            String sessionId = req.cookie("sessionId");
            if (sessionId != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del("session:" + sessionId);
                    jedis.del("csrf:" + sessionId);
                }
            }
            res.removeCookie("sessionId");
            res.redirect("/login.html");
            return null;
        });
        // Graceful shutdown: close JedisPool when Spark stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jedisPool != null) {
                jedisPool.close();
            }
        }));
    }
}
