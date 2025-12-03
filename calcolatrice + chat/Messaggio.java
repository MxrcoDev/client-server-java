import java.io.Serializable;

public class Messaggio implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int mittente;
    private String messaggio;

    public Messaggio(int m, String msg) {
        this.mittente = m;
        this.messaggio = msg;
    }

    public String get() {
        return "[ CLIENT " + mittente + " ] : " + messaggio;
    }
    
    public int getMittente() {
        return mittente;
    }
    
    public String getTesto() {
        return messaggio;
    }
}