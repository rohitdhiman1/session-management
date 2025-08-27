package com.rohitdhiman;

import spark.Spark;
import spark.Request;
import spark.Response;
import redis.clients.jedis.Jedis;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class SessionManager {

    private static Jedis jedis;

    public static void main(String[] args) {
        // Configure Spark to use a fixed thread pool.
        Spark.threadPool(20);
        Spark.port(4567);

        // Get Redis host from environment variable
        String redisHost = System.getenv("REDIS_HOST");
        System.out.println("Connecting to Redis at: " + redisHost);
        jedis = new Jedis(redisHost);
        System.out.println("Connected to Redis!");

        // Route to serve the login page
        Spark.get("/", (req, res) -> {
            res.redirect("/login");
            return null;
        });

        // Route to handle login form submission
        Spark.post("/login", (req, res) -> {
            // Hardcoded credentials for this example
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if ("user".equals(username) && "password".equals(password)) {
                // Generate a unique session ID
                String sessionId = UUID.randomUUID().toString();
                String redisKey = "session:" + sessionId;

                // Store session data in Redis with a 15-minute expiration
                // EX stands for seconds
                jedis.setex(redisKey, TimeUnit.MINUTES.toSeconds(15), username);

                // Set the session ID as a cookie in the user's browser
                res.cookie("sessionId", sessionId, 15 * 60); // 15-minute cookie lifetime

                // Redirect to the dashboard
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
                // No session ID, redirect to login
                res.redirect("/login");
                return null;
            }

            String username = jedis.get("session:" + sessionId);
            if (username == null) {
                // Session ID found but no data in Redis (expired or invalid)
                res.removeCookie("sessionId");
                res.redirect("/login");
                return null;
            }

            // Session is valid, render the dashboard content
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
                // Invalidate the session in Redis
                jedis.del("session:" + sessionId);
            }
            // Remove the cookie from the user's browser
            res.removeCookie("sessionId");
            res.redirect("/login");
            return null;
        });
    }
}
