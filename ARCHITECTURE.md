# **Service Architecture**

---

## **Table of Contents**

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Sequence Diagrams](#sequence-diagrams)
4. [Rough Directory Structure](#rough-directory-structure)

---

## **Overview**

This Project includes a **Next.js frontend** served by a **RESTful API** built using **Java Spring Boot**. It follows a **Layered Architecture** inspired by the **MVC (Model-View-Controller)** pattern, enhanced with additional layers for better separation of concerns and maintainability. This README demonstrates how to structure a Spring Boot application with **Model**, **Repository**, **Service**, and **Controller** layers.

---

## **Architecture**

### **1. Model Layer**

**Purpose**: Represents the application's data structures and business objects.

**Components**:
- **Entities/POJOs**: Define the data attributes and relationships.
- **Data Transfer Objects (DTOs)**: Facilitate data exchange between layers without exposing internal models.
- **Models**:
  - TBC

### **2. Repository Layer**

**Purpose**: Handles data persistence and retrieval, interacting directly with the database.

**Components**:
- **Repository Interfaces**: Extend Spring Data JPA interfaces to provide CRUD operations and custom queries.
    - TBC

### **3. Service Layer**

**Purpose**: Encapsulates business logic, acting as an intermediary between the Controller and Repository layers.

**Components**:
- **Service Classes**: Contain methods that define business operations, handle transactions, and coordinate complex processes.
  - `ImageUploadService`
  - `CropService`
  - `BackgroundRemovalService`
  - `ExportService`

### **4. Controller Layer**

**Purpose**: Manages HTTP requests, maps them to service methods, and returns responses to the client.

**Components**:
- **REST Controllers**: Annotated with `@RestController`, they define API endpoints and handle request/response lifecycle.
  - `ImageUploadController`
  - `CropController`
  - `BackgroundRemovalController`
  - `ExportController`

---
## **Sequence Diagrams**

### Image Upload Functionality:
```plantuml
@startuml
actor User
participant "ImageUploadController" as C_Upload
participant "ImageUploadService" as S_Upload
participant "ImageRepository" as Repository
database "File System Storage" as FS

User -> C_Upload: POST /api/images/upload (imageFile, backgroundOption, customBackground)
activate C_Upload

C_Upload -> S_Upload: processImage(imageFile, backgroundOption, customBackground)
activate S_Upload

S_Upload -> FS: Save original image to <storagePath>/<uuid-generated-imageId>.jpg
note right: Stores the uploaded image file in `backend/images` folder

S_Upload -> Repository: save(ImageData)
note right: Persists metadata to supabase postgres db

S_Upload -->> C_Upload: Response (imageId, status="FAILED" / "UPLOADED", message)
deactivate S_Upload

C_Upload -->> User: 200 OK/400 Bad Req/500 ISE Response (imageId, status="FAILED" / "UPLOADED", message)
deactivate C_Upload
@enduml
```
---

## **Rough Directory Structure**

```plaintext
example-spring-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── controller/
│   │   │           │   └── SomeController.java
│   │   │           ├── model/
│   │   │           │   └── SomeModel.java
│   │   │           ├── repository/
│   │   │           │   └── SomeRepository.java
│   │   │           └── service/
│   │   │               └── SomeService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── UserControllerTest.java
├── .dockerignore
├── Dockerfile
├── pom.xml
└── README.md
```
