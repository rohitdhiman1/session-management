# session-management

Session Management with SparkJava, Redis, and Docker
This project demonstrates a simple, containerized web application that uses SparkJava for its web framework and Redis for session management. It's a foundational example of how to handle user sessions in a scalable and robust way.

Key Features
User Authentication: A basic login system with hardcoded credentials.

Session Management: Creates and manages user sessions, storing them in a Redis database.

Session-based Authorization: Protects a dashboard page, allowing access only to logged-in users.

Containerized Environment: Uses Docker and Docker Compose to run the Java application and the Redis database in a separate, isolated environment, making it easy to set up and run.

Technologies Used
Java 17: The programming language for the application.

Maven: A build tool for managing project dependencies and creating the executable JAR.

SparkJava: A lightweight framework for creating web applications in Java.

Jedis: A popular Java client for interacting with Redis.

Redis: An in-memory key-value store used for high-performance session storage.

Docker: Used to build the application and run both the Java app and Redis in containers.

Setup and Running the Project
Clone the repository:

git clone https://github.com/rohitdhiman/session-management.git
cd session-management

Build and run the containers:
Docker Compose will build the Java application image, download the Redis image, and start both containers in a shared network.

docker-compose up --build

Access the application:
Open your web browser and navigate to:

http://localhost:4567

Credentials
The application uses hardcoded login credentials for demonstration purposes.


Once logged in, you will be redirected to the protected dashboard page. Your session will be stored in Redis and will expire after 15 minutes.