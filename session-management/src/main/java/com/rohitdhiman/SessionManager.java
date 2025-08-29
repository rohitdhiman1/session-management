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

public class SessionManager {

    private static JedisPool jedisPool;

    public static void main(String[] args) {
        // Configure Spark to use a fixed thread pool.
        Spark.threadPool(20);
        Spark.port(4567);

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

        // Route to serve the login page
        Spark.get("/", (req, res) -> {
            res.redirect("/login");
            return null;
        });

        // Route to handle login form submission
        Spark.post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if ("user".equals(username) && "password".equals(password)) {
                String sessionId = UUID.randomUUID().toString();
                String redisKey = "session:" + sessionId;

                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.setex(redisKey, TimeUnit.MINUTES.toSeconds(15), username);
                }

                res.cookie("sessionId", sessionId, 15 * 60);
                res.redirect("/dashboard");
                return null;
            } else {
                return "Invalid credentials. <a href='/login'>Try again</a>.";
            }
        });

        // Protected dashboard route
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

            return "<html><body>" +
                   "<h1>Welcome, " + username + "!</h1>" +
                   "<p>This is a protected page. Your session is active.</p>" +
                   "<a href='/logout'>Logout</a>" +
                   "</body></html>";
        });

        // Route to serve the login page (simple form)
        Spark.get("/login", (req, res) -> {
            return "<html><body>" +
                   "<h1>Login Page</h1>" +
                   "<form method='post' action='/login'>" +
                   "Username: <input type='text' name='username'><br>" +
                   "Password: <input type='password' name='password'><br>" +
                   "<input type='submit' value='Login'>" +
                   "</form>" +
                   "</body></html>";
        });

        // Logout route
        Spark.get("/logout", (req, res) -> {
            String sessionId = req.cookie("sessionId");
            if (sessionId != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del("session:" + sessionId);
                }
            }
            res.removeCookie("sessionId");
            res.redirect("/login");
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
