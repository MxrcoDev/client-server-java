import java.io.Serializable;   // Importa l'interfaccia per rendere la classe serializzabile

public class Operazione implements Serializable {  
    // La classe implementa Serializable perché deve essere inviata tramite rete
    // Serializzare = trasformare l'oggetto in una sequenza di byte

    private static final long serialVersionUID = 1L;
    // Versione della classe per la serializzazione
    // Serve a garantire compatibilità durante la lettura degli oggetti

    public String tipo;      // Tipo di operazione: "+", "-", "*", "/"
    public double a;         // Primo operando
    public double b;         // Secondo operando

    // Costruttore: inizializza tutti i campi
    public Operazione(String tipo, double a, double b) {
        this.tipo = tipo;
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        // Restituisce una rappresentazione leggibile dell'operazione
        // Esempio: "5 + 3"
        return a + " " + tipo + " " + b;
    }
}
