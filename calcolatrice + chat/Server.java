import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static int clientCounter = 0;
    private static Buffer chatBuffer = new Buffer();
    
    private static List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server in ascolto sulla porta 5000...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                int clientId;
                synchronized (Server.class) {
                    clientId = ++clientCounter;
                }
                
                System.out.println("Client connesso! ID = " + clientId);

                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Classe interna per gestire ogni client
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;
        private ObjectInputStream inObj;
        private ObjectOutputStream outObj;
        private volatile boolean running = true;

        public ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
        }

        public void inviaCodata() {
            try {
                synchronized(chatBuffer) {
                    outObj.reset();
                    outObj.writeObject(chatBuffer.get());
                    outObj.flush();
                }
            } catch (IOException e) {
                running = false;
            }
        }

        @Override
        public void run() {
            try {
                outObj = new ObjectOutputStream(clientSocket.getOutputStream());
                inObj = new ObjectInputStream(clientSocket.getInputStream());

                outObj.writeObject(clientId);
                outObj.flush();

                System.out.println("Client " + clientId + " pronto: " + clientSocket.getInetAddress());

                while (running) {
                    try {
                        Object obj = inObj.readObject();

                        if (obj instanceof Operazione) {
                            Operazione op = (Operazione) obj;

                            double risultato = switch (op.tipo) {
                                case "+" -> op.a + op.b;
                                case "-" -> op.a - op.b;
                                case "*" -> op.a * op.b;
                                case "/" -> op.b != 0 ? (double) op.a / op.b : 0;
                                default -> 0;
                            };

                            outObj.writeObject(op.toString() + " = " + risultato);
                            outObj.flush();
                        }
                        else if (obj instanceof Messaggio) {
                            Messaggio msg = (Messaggio) obj;
                            chatBuffer.put(msg);
                            
                            // Invia conferma
                            outObj.writeObject("MSG_OK");
                            outObj.flush();
                            
                            // NON fare broadcast qui!
                            // Il client richiederà la coda con RICHIESTA_CODA
                        }
                        else if (obj instanceof String) {
                            String cmd = (String) obj;
                            if(cmd.equals("RICHIESTA_CODA")) {
                                inviaCodata();
                            }
                        }

                    } catch (EOFException e) {
                        System.out.println("Client " + clientId + " disconnesso.");
                        break;
                    }
                }

            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnesso.");
            } finally {
                running = false;
                clientHandlers.remove(this);
                try { clientSocket.close(); } catch (Exception ignored) {}
            }
        }
    }
}