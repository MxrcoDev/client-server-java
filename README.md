![header](https://capsule-render.vercel.app/api?type=waving&height=230&color=gradient&customColorList=7&text=Java%20Calc%20Client-Server&textBg=false&fontAlignY=40&reversal=false&animation=fadeIn)

![Java](https://img.shields.io/badge/Language-Java-red) ![Version](https://img.shields.io/badge/Version-1.0-green)

**Java Calc Client-Server** is a Java project that implements a simple client-server application. Clients can insert mathematical operations, send them to the server, and receive the results in real-time. Multiple clients can connect simultaneously, and each operation is handled in a separate thread.

---

## ❗ Dependencies
[![Java](https://img.shields.io/badge/Dependency-Java-blue?style=for-the-badge&logo=java)](https://www.java.com/)

---

## 🕹️ How It Works?
- The **Server** listens on a specified port (default: 5000) and can handle multiple clients concurrently.  
- **Clients** can:
  - Insert arithmetic operations (addition, subtraction, multiplication, division).  
  - View the list of operations locally.  
  - Send operations to the server to receive results.  
- Each operation is processed in a **separate thread** to allow concurrent execution.

---

## ⚙️ Features
- Supports basic arithmetic operations: `+`, `-`, `*`, `/`.  
- Multithreaded server for handling multiple clients at once.  
- Client-side interface using a simple console menu.  
- Operations are stored locally on the client until they are sent to the server.  

---

## 🔧 How To Run
### On Windows
1. Open the project folder.  
2. Run `start.bat` to compile and launch the server and client.  
