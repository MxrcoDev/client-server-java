import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    // Contatore dei client, condiviso tra tutti i thread.
    // Serve per assegnare un ID univoco. Protetto tramite synchronized.
    private static int clientCounter = 0;

    // Buffer condiviso tra tutti i client per la chat.
    // È un monitor: i metodi put() e get() sono synchronized.
    private static Buffer chatBuffer = new Buffer();
    
    // Lista dei client connessi, resa thread-safe con synchronizedList
    private static List<ClientHandler> clientHandlers =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server in ascolto sulla porta 5000...");

            while (true) {
                // Attende un nuovo client
                Socket clientSocket = serverSocket.accept();

                int clientId;
                // Incremento atomico del clientCounter
                synchronized (Server.class) {
                    clientId = ++clientCounter;
                }
                
                System.out.println("Client connesso! ID = " + clientId);

                // Crea un nuovo gestore (thread) per questo client
                ClientHandler handler = new ClientHandler(clientSocket, clientId);

                // Aggiunge alla lista thread-safe
                clientHandlers.add(handler);

                // Avvia il thread dedicato
                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================
    // THREAD DEDICATO A OGNI CLIENT
    // ==============================

    static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private int clientId;

        private ObjectInputStream inObj;
        private ObjectOutputStream outObj;

        // Variabile volatile per fermare il thread in modo sicuro
        private volatile boolean running = true;

        public ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
        }

        // Invia l'intera coda della chat al client che l'ha chiesta.
        // Synchronized su chatBuffer per evitare conflitti con put().
        public void inviaCodata() {
            try {
                synchronized(chatBuffer) {
                    outObj.reset();  // Resetta la cache della serializzazione
                    outObj.writeObject(chatBuffer.get()); // Invia copia della coda
                    outObj.flush();   // Forza l'invio immediato
                }
            } catch (IOException e) {
                running = false;
            }
        }

        @Override
        public void run() {
            try {
                // Stream per la comunicazione oggetti → necessari per ObjectInput/OutputStream
                outObj = new ObjectOutputStream(clientSocket.getOutputStream());
                inObj  = new ObjectInputStream(clientSocket.getInputStream());

                // Invia ID del client assegnato
                outObj.writeObject(clientId);
                outObj.flush();

                System.out.println("Client " + clientId +
                                   " pronto: " + clientSocket.getInetAddress());

                // =======================================
                // LOOP PRINCIPALE DEL THREAD DEL CLIENT
                // =======================================
                while (running) {
                    try {
                        Object obj = inObj.readObject(); // Attende messaggi/operazioni

                        // -------------------------------
                        // 1. Il client ha inviato un'operazione matematica
                        // -------------------------------
                        if (obj instanceof Operazione) {
                            Operazione op = (Operazione) obj;

                            // Calcolo dell’operazione
                            double risultato = 0;

                            switch (op.tipo) {
                                case "+":
                                    risultato = op.a + op.b;
                                    break;
                                case "-":
                                    risultato = op.a - op.b;
                                    break;
                                case "*":
                                    risultato = op.a * op.b;
                                    break;
                                case "/":
                                    risultato = op.b != 0 ? op.a / op.b : Double.NaN;
                                    break;
                                default:
                                    risultato = 0;
                                    break;
                            }


                            // Invia la stringa risultato al client
                            outObj.writeObject(op.toString() + " = " + risultato);
                            outObj.flush();
                        }

                        // -------------------------------
                        // 2. Il client ha inviato un messaggio della chat
                        // -------------------------------
                        else if (obj instanceof Messaggio) {
                            Messaggio msg = (Messaggio) obj;

                            // Inserisce il messaggio nel buffer condiviso (operazione sincronizzata)
                            chatBuffer.put(msg);
                            
                            // Conferma ricezione al client
                            outObj.writeObject("MSG_OK");
                            outObj.flush();

                            // Nessun broadcast immediato:
                            // saranno i client a richiedere "RICHIESTA_CODA"
                        }

                        // -------------------------------
                        // 3. Il client chiede la coda della chat
                        // -------------------------------
                        else if (obj instanceof String) {
                            String cmd = (String) obj;
                            if (cmd.equals("RICHIESTA_CODA")) {
                                inviaCodata();
                            }
                        }

                    } catch (EOFException e) {
                        // Client ha chiuso la connessione in modo pulito
                        System.out.println("Client " + clientId + " disconnesso.");
                        break;
                    }
                }

            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnesso.");
            }

            // Uscita dal thread
            finally {
                running = false;
                clientHandlers.remove(this);
                try { clientSocket.close(); } catch (Exception ignored) {}
            }
        }
    }
}
