# Medical-Insurance-System

# BigDataIndexing

## Project Overview
This project handles **medical coverage plan information** by indexing structured JSON objects using **Spring Boot**, **Redis**, **Elasticsearch**, and **RabbitMQ**. It incorporates **Google OAuth 2.0** for secure authentication and supports full CRUD operations with validation, caching, and asynchronous message processing. using **Spring Boot**, **Redis**, **Elasticsearch**, and **RabbitMQ**.

---

## Tech Stack
- **Spring Boot (Java)**: Backend framework
- **Redis**: Key-value store for caching and persistence
- **Elasticsearch**: Full-text search engine for indexing
- **RabbitMQ**: Message broker for asynchronous processing
- **Google OAuth 2.0**: Authentication for API security

---

## Features
1. **Authentication**
   - Secured API with Google OAuth 2.0 authentication.

2. **Request Validation**
   - Validates JSON objects against a defined JSON Schema.

3. **Caching with ETag**
   - Server responses are cached and validated using the `ETag` header.

4. **REST API**
   - Supports HTTP methods: **POST**, **PUT**, **PATCH**, **GET**, **DELETE**.

5. **Data Persistence**
   - Stores structured JSON objects in **Redis** for quick access.

6. **Indexing**
   - Indexes data into **Elasticsearch** for search capabilities.

7. **Message Queueing**
   - Uses **RabbitMQ** to handle indexing requests asynchronously.

---

## Data Flow

1. **Authentication**  
   - Users authenticate via **Google OAuth 2.0**.

2. **API Requests**  
   - API requests are validated using OAuth tokens.

3. **JSON Object Creation**  
   - JSON objects are created using the **POST** method.

4. **Schema Validation**  
   - Each JSON object is validated against its respective schema.

5. **Data Persistence**  
   - JSON objects are stored in **Redis** after being de-structured.

6. **Queueing for Indexing**  
   - JSON objects are enqueued in **RabbitMQ** for indexing.

7. **Indexing in Elasticsearch**  
   - **RabbitMQ** dequeues and sends objects for indexing into **Elasticsearch**.

8. **Search Queries**  
   - Search indexed data through **Kibana Console**.

---

## API Endpoints

- **GET /plan**  
   - Fetches all plans.
  
- **POST /plan**  
   - Creates a new plan from the request body.

- **PUT /plan/{id}**  
   - Updates an existing plan identified by the `id`.  
   - Requires a valid ETag in the `If-Match` header.
  
- **PATCH /plan/{id}**  
   - Partially updates an existing plan identified by the `id`.  
   - Requires a valid ETag in the `If-Match` header.
  
- **GET /plan/{id}**  
   - Fetches an existing plan by the `id`.  
   - Optionally provides an ETag in the `If-None-Match` header.  
   - Returns an ETag in the response header for caching.
  
- **DELETE /plan/{id}**  
   - Deletes the plan identified by the `id`.  
   - Requires a valid ETag in the `If-Match` header.

---

## Prerequisites

Make sure to install the following before running the project:

- [Redis](https://redis.io/download)
- [Elasticsearch](https://www.elastic.co/downloads/elasticsearch)
- [Kibana](https://www.elastic.co/downloads/kibana)
- [RabbitMQ](https://www.rabbitmq.com/download.html)

---

## Steps to Run

1. **Start the Servers**  
   - Start Redis, Elasticsearch, Kibana, and RabbitMQ by running:
     ```bash
     redis-server
     elasticsearch
     kibana
     rabbitmq-server
     ```

2. **Run the Application**  
   - Start the Spring Boot application.

3. **Use the API**  
   - Use the provided REST API endpoints to create and manage data.

4. **Search Data**  
   - Query indexed data via the **Kibana Console** for search operations.
