
import java.util.LinkedList;
import java.util.Queue;

public class Buffer {
    private Queue<Messaggio> coda = new LinkedList<>();
    private int ids = 0;
    
    public synchronized void put(Messaggio m){
        coda.add(m);
        notifyAll();
    }

    public synchronized Queue<Messaggio> get() {
        return new LinkedList<>(coda);
    }
    
}
