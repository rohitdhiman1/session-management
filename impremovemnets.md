# Improvements: Using JedisPool for Redis Connections

## What Changed

The application now uses `JedisPool` (a Redis connection pool) instead of a single `Jedis` instance for all Redis operations.

## Value and Benefits

- **Thread Safety**: `Jedis` instances are not thread-safe. Using a single instance in a multi-threaded web server can cause unpredictable errors. `JedisPool` provides a safe way for multiple threads to access Redis concurrently.
- **Scalability**: Connection pooling allows the application to handle more simultaneous requests efficiently, as each thread can borrow a connection from the pool.
- **Resource Management**: Connections are reused and managed by the pool, reducing overhead and improving performance.
- **Graceful Shutdown**: The pool can be closed cleanly when the application stops, preventing resource leaks.

## Summary

Switching to `JedisPool` makes the application more robust, scalable, and production-ready by ensuring safe and efficient Redis access in a multi-threaded environment.
