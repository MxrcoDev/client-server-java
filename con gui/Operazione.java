public class Operazione {
    
    private double a;
    private double b;
    private String op;

    public Operazione(double a, double b,String op) {
        this.a = a;
        this.b = b;
        this.op=op;
    }

    public double risultato() {
        return switch(op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if(b == 0)
                    throw new ArithmeticException("Divisione per zero");
                else if (a == 0 && b == 0) {
                    System.out.println("Indeterminato");
                }
                    yield a / b;
            }
            default -> throw new IllegalArgumentException("Operatore non valido");
        };
    }

    public String toString() {
        return a+" "+op+" "+b;
    }

}
