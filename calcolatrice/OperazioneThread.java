import java.io.*;      // Importa classi per stream e serializzazione
import java.net.*;     // Importa classi per Socket e comunicazione di rete

public class OperazioneThread extends Thread {

    private Operazione op;   // L'operazione da inviare al server
    private String host;     // Indirizzo del server (es. "localhost")
    private int port;        // Porta su cui il server è in ascolto

    // Costruttore: riceve l'operazione, l'host e la port
    public OperazioneThread(Operazione op, String host, int port) {
        this.op = op;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        // Metodo eseguito quando viene avviato il thread (start())

        try (Socket socket = new Socket(host, port)) {
            // Crea una nuova connessione al server
            // Ogni operazione ha la sua connessione indipendente

            ObjectOutputStream outObj = new ObjectOutputStream(socket.getOutputStream());
            // Stream per inviare l'oggetto Operazione al server

            ObjectInputStream inObj = new ObjectInputStream(socket.getInputStream());
            // Stream per ricevere la risposta dal server

            // --- INVIO DELL'OPERAZIONE ---
            outObj.writeObject(op);   // Serializza e invia l'oggetto "Operazione"
            outObj.flush();           // Forza l'invio immediato

            // --- RICEZIONE DEL RISULTATO ---
            String risultato = (String) inObj.readObject();
            // Attende l'oggetto inviato dal server e lo converte in String

            // Stampa l'operazione e il risultato ricevuto
            System.out.println(op + " = " + risultato);

        } catch (Exception e) {
            // Segnala errori di connessione o serializzazione
            System.out.println("Errore invio operazione: " + op);
            e.printStackTrace();
        }
    }
}
