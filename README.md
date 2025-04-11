# IS442 Project

## **Frontend Setup**
```
cd frontend

npm i # install packages

npm run dev # run local dev server
```

## **Backend Setup Quickstart**

### **Prerequisites**

- **Java 21**: Ensure Java 21 is installed.
  ```bash
  java -version
  ```

- **IntelliJ**

### **Using IntelliJ IDE:**
- Use Intellij IDE to easily build and run the Spring Boot application.
- Ensure that you open `backend` directory as the project root in IntelliJ.
- Go to Run > Edit Configurations.
    - Under Modify Options > Environment Variables, add: (This is for your supabase connection & google drive integration)
      ```
      DATABASE_URL=your-database-url
      DATABASE_USERNAME=postgres
      DATABASE_PASSWORD=your-password
      GOOGLE_CLIENT_ID=your-google-client-id
      GOOGLE_CLIENT_SECRET=your-google-client-secret
      REDIRECT_URI=redirect-uri
      ```
- Click the Run icon at the top right corner.

### **Accessing the Service locally**

- **API Endpoint**: [http://localhost:8080](http://localhost:8080)

---


## **Contributing**
- Refer to CONTRIBUTING.md
