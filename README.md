git clone https://github.com/rohitdhiman/session-management.git

# 🚀 Session Management Example

>A simple, containerized web application using **SparkJava** and **Redis** for session management. This project demonstrates scalable user session handling in Java, with everything running in Docker for easy setup.

---

## ✨ Key Features
- **User Authentication:** Basic login system with hardcoded credentials
- **Session Management:** User sessions stored in Redis with expiration
- **Session-based Authorization:** Protected dashboard for logged-in users
- **Containerized Environment:** Java app & Redis run in Docker Compose

---

## 🛠️ Technologies Used
- **Java 17**
- **Maven** (build tool)
- **SparkJava** (web framework)
- **Jedis** (Redis client)
- **Redis** (session storage)
- **Docker & Docker Compose**

---

## Runbook: Local Development & Testing

### Prerequisites
- Java 17 (or higher)
- Maven
- Docker & Docker Compose

### Clone the Repository
```sh
git clone https://github.com/rohitdhiman/session-management.git
cd session-management
```

### Build the Project
You can build the Java application using Maven:
```sh
cd session-management
mvn clean package
```
This will create a JAR file in the `target/` directory.

### Run with Docker Compose
Docker Compose will build the Java application image, download the Redis image, and start both containers in a shared network.
```sh
docker-compose up --build
```

### Access the Application
Open your web browser and navigate to:
```
http://localhost:4567
```

### Credentials
The application uses hardcoded login credentials for demonstration purposes:
- **Username:** user
- **Password:** password

### Testing Locally
1. Go to the login page at `http://localhost:4567/login`.
2. Enter the credentials above.
3. On successful login, you will be redirected to the protected dashboard page.
4. Your session will be stored in Redis and will expire after 15 minutes.
5. You can log out using the "Logout" link on the dashboard.

### Stopping the Application
Press `Ctrl+C` in the terminal running Docker Compose to stop the containers.

### Troubleshooting
- Ensure Docker and Docker Compose are installed and running.
- If the app does not start, check the logs for errors (e.g., Redis connection issues).
- Make sure port 4567 is not blocked by another process.

---