import java.util.LinkedList;
import java.util.Queue;

// Classe Buffer utilizzata come monitor condiviso tra tutti i client.
// Serve a memorizzare i messaggi della chat in modo thread-safe.
// Tutti gli accessi alla coda avvengono tramite metodi synchronized.
public class Buffer {

    // Coda FIFO che contiene i messaggi della chat
    private Queue<Messaggio> coda = new LinkedList<>();

    // Variabile non usata attualmente, lasciata per eventuali sviluppi (es. ID messaggi)
    private int ids = 0;
    
    // Inserisce un nuovo messaggio nella coda.
    // Metodo synchronized: garantisce l'accesso esclusivo tra thread.
    // notifyAll() serve nel caso ci fossero thread in attesa (non utilizzato qui ma corretto per un monitor).
    public synchronized void put(Messaggio m){
        coda.add(m);
        notifyAll(); // Sveglia eventuali thread che attendono nuovi messaggi
    }

    // Restituisce una COPIA della coda dei messaggi.
    // Metodo synchronized: evita condizioni di race durante la lettura.
    // Restituiamo una nuova LinkedList per evitare che il chiamante possa modificare la coda originale.
    public synchronized Queue<Messaggio> get() {
        return new LinkedList<>(coda);
    }
    
}
