import java.io.*;     // Importa le classi per gestire input/output (stream)
import java.net.*;    // Importa le classi per rete: ServerSocket, Socket

public class Server {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {  
            // Crea il server in ascolto sulla porta 5000
            // Il try-with-resources chiuderà automaticamente il serverSocket

            System.out.println("Server in ascolto sulla porta 5000...");

            while (true) {  
                // Ciclo infinito per accettare più connessioni

                Socket clientSocket = serverSocket.accept();  
                // Il server rimane bloccato finché un client non si connette
                System.out.println("Client connesso!");

                // Avvia un nuovo thread per gestire ogni client indipendentemente
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (Exception e) {
            // Se si verificano errori nella creazione del server
            e.printStackTrace();
        }
    }

    // Metodo che gestisce ogni singolo client
    private static void handleClient(Socket clientSocket) {
        try (
            ObjectInputStream inObj = new ObjectInputStream(clientSocket.getInputStream());
            // Stream per ricevere oggetti dal client

            ObjectOutputStream outObj = new ObjectOutputStream(clientSocket.getOutputStream());
            // Stream per inviare oggetti al client
        ) {

            // Riceve l'oggetto Operazione inviato dal client
            Operazione op = (Operazione) inObj.readObject();

            // Calcola il risultato in base al tipo di operazione
            double risultato = switch (op.tipo) {
                case "+" -> op.a + op.b;  // Somma
                case "-" -> op.a - op.b;  // Sottrazione
                case "*" -> op.a * op.b;  // Moltiplicazione
                case "/" -> op.a / op.b;  // Divisione (intera)
                default -> 0;             // Operazione non riconosciuta
            };

            // Invia il risultato al client come stringa
            outObj.writeObject(String.valueOf(risultato));
            outObj.flush();  // Forza l'invio immediato

            System.out.println("Operazione eseguita: " + op + " = " + risultato);

        } catch (Exception e) {
            // Gestione errori nella comunicazione con il client
            System.out.println("Errore durante la gestione del client.");
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();  // Chiude la connessione con il client
            } catch (Exception ignored) {}
        }
    }
}
