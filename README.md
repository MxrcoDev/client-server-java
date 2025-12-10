![header](https://capsule-render.vercel.app/api?type=waving&height=230&color=gradient&customColorList=7&text=Java%20Calc%20Client-Server&textBg=false&fontAlignY=40&reversal=false&animation=fadeIn)

![Java](https://img.shields.io/badge/Language-Java-red) ![Version](https://img.shields.io/badge/Version-6.5-green)

**Java Calc Client-Server** is a Java project implementing a concurrent client-server system.  
Clients can insert mathematical operations, send them to the server, and receive results in real-time. Multiple clients can connect simultaneously, and each client is handled in a separate thread. In addition, clients can participate in a shared chat.

## ❗ Dependencies
[![Java](https://img.shields.io/badge/Dependency-Java-blue?style=for-the-badge&logo=java)](https://www.java.com/) 
Version 14

## 🕹️ How It Works?
### Server
- Listens on port `5000` by default.
- Handles multiple clients concurrently using `ClientHandler` threads.
- Computes results for arithmetic operations received from clients.
- Maintains a shared buffer for chat messages using a synchronized queue.

### Client
- Connects to the server and receives a unique client ID.
- Provides a console-based menu to:
  - Insert arithmetic operations (`+`, `-`, `*`, `/`)  
  - View operations stored locally  
  - Send operations to the server and receive results  
  - Participate in a shared chat
- Maintains a local history of results, distinguishing between old and recent calculations.

---

## ⚙️ Features
- **Arithmetic Operations:** addition, subtraction, multiplication, division.  
- **Multithreading:** each client is managed in a separate thread on the server.  
- **Concurrency Control:** server uses synchronized methods to manage access to shared chat buffer.  
- **Client-Side Storage:** operations are stored locally until sent; results are kept for review.  
- **Shared Chat:** multiple clients can send and receive messages without conflicts.  
- **Serialization:** operations and messages are sent over the network as serializable Java objects.

---

## 🗂️ Project Structure
