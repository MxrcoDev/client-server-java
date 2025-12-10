import java.io.Serializable;

// Classe che rappresenta un messaggio spedito tramite la chat condivisa.
// Implementa Serializable per permettere l'invio dell'oggetto attraverso i socket.
public class Messaggio implements Serializable {
    private static final long serialVersionUID = 1L;   // Versione della classe per la serializzazione
    
    private int mittente;      // ID del client che ha inviato il messaggio
    private String messaggio;  // Testo del messaggio inviato

    // Costruttore: crea un messaggio con ID mittente + testo
    public Messaggio(int m, String msg) {
        this.mittente = m;
        this.messaggio = msg;
    }

    // Metodo che restituisce una rappresentazione formattata del messaggio.
    // Usato dal client per stampare la chat.
    public String get() {
        return "[ CLIENT " + mittente + " ] : " + messaggio;
    }
    
    // Restituisce l'ID del mittente - utile se il server deve identificarlo
    public int getMittente() {
        return mittente;
    }
    
    // Restituisce il contenuto testuale del messaggio
    public String getTesto() {
        return messaggio;
    }
}
