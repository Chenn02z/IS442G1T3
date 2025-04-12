# IS442 Project

## **Frontend Setup**
### **Prerequisites**
- Ensure that `npm` is installed on your machine
```
cd frontend

npm i # install packages

npm run dev # run local dev server
```
- The Next.js server will start on port 3000
- **The frontend UI can be accessed at:** [http://localhost:3000](http://localhost:3000)
## **Backend Setup**

### **Prerequisites**

- **Java 21**: Ensure Java 21 is installed.
  ```bash
  java -version
  ```

- **IDE**: You can use either IntelliJ or VS Code

- Create a `.env` file in the same directory (same level as `pom.xml`) and paste the following:
    ```properties
    DATABASE_URL=your-database-url
    DATABASE_USERNAME=postgres
    DATABASE_PASSWORD=your-password
    GOOGLE_CLIENT_ID=your-google-client-id
    GOOGLE_CLIENT_SECRET=your-google-client-secret
    REDIRECT_URI=redirect_URI
    ```

### **Using CLI with Maven**

- **Maven**: Ensure Maven is installed and `mvn` is available in your terminal.
  ```bash
  mvn -v
  ```

1. Ensure you are inside the `backend` directory.

2. Start the backend:
    ```bash
    mvn clean spring-boot:run
    ```
### **Using IntelliJ IDE:**

1. Open the `/backend` directory as the project root in IntelliJ.
2. Set up environment variables in run configurations (in `Edit Run Configuration`)
3. Click the Run icon at the top right corner.

### **Accessing the Backend Service Locally**

- **API Endpoint**: [http://localhost:8080](http://localhost:8080)
- Swagger API Docs: [http://localhost:8080/swagger](http://localhost:8080/swagger)

## **Contributing**
- Refer to [CONTRIBUTING.md](CONTRIBUTING.md)
