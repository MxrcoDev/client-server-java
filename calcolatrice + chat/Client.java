import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Client {

    static Queue<Operazione> listaOperazioni = new LinkedList<>();
    static List<String> listaRisultati = new ArrayList<>();

    // Variabili per indicare l'indice della prima e dell'ultima operazione di un burst di invio
    static int primaOperazione = 0;
    static int ultimaOperazione = 0;

    static Queue<Messaggio> listaMessaggi = new LinkedList<>();

    public static void inserisci(Scanner s, ClearScreen screen) {
        int option;

        do {
            screen.clear();
            System.out.println("----- INSERISCI OPERAZIONE -----");
            System.out.println("\n1 - Somma");
            System.out.println("2 - Sottrazione");
            System.out.println("3 - Moltiplicazione");
            System.out.println("4 - Divisione");
            System.out.println("5 - Esci");

            System.out.print("\nInserisci: ");
            option = s.nextInt();
            s.nextLine();

            if(option >= 1 && option <= 4) {

                System.out.print("Primo numero: ");
                int a = s.nextInt();
                System.out.print("Secondo numero: ");
                int b = s.nextInt();
                s.nextLine();

                String tipo = "";
                switch(option) {
                    case 1: tipo = "+"; break;
                    case 2: tipo = "-"; break;
                    case 3: tipo = "*"; break;
                    case 4: tipo = "/"; break;
                }

                listaOperazioni.add(new Operazione(tipo, a, b));
                System.out.println("Operazione aggiunta: " + a + " " + tipo + " " + b);

                System.out.println("\nPremi invio per continuare...");
                s.nextLine();

            } else if(option == 5) {
                System.out.println("Ritorno al menu principale...");
            } else {
                System.out.println("Opzione non valida.");
            }

        } while(option != 5);
    }

    public static void visualizzaMessaggi(ClearScreen screen, ObjectInputStream inObj, ObjectOutputStream outObj) {

        try {
            outObj.writeObject("RICHIESTA_CODA"); 
            outObj.flush();
            Object obj = inObj.readObject();
            if(obj instanceof Queue) {
                Queue<Messaggio> coda = (Queue<Messaggio>) obj;
                listaMessaggi.clear();
                listaMessaggi.addAll(coda);
            }
        } catch (Exception e) {}

        screen.clear();
        System.out.println("----- CHAT -----");
        if (!listaMessaggi.isEmpty()) {
            for(Messaggio m : listaMessaggi) {
                System.out.println(m.get());
            }
        }

        System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
    }
    
    public static void chat(Scanner s, ClearScreen screen, ObjectInputStream inObj, ObjectOutputStream outObj, int clientId) {
        boolean exitChat = false;
        
        visualizzaMessaggi(screen, inObj, outObj);
    
        while(!exitChat) {
            String text = s.nextLine();
    
            if(text.equals("exit")) {
                exitChat = true;
            } else if (text.equals("1")) {
                visualizzaMessaggi(screen, inObj, outObj);
            } else if(!text.trim().isEmpty()) {
                
                System.out.print("\nInvio in corso...");
                
                try {
                    // Invia il messaggio
                    outObj.writeObject(new Messaggio(clientId, text));
                    outObj.flush();
                    
                    // ASPETTA LA CONFERMA DAL SERVER (con timeout)
                    Object conferma = inObj.readObject();
                    
                    if(conferma instanceof String && conferma.equals("MSG_OK")) {
                        // Aggiorna la visualizzazione
                        visualizzaMessaggi(screen, inObj, outObj);
                    } else {
                        System.out.println(" Errore!");
                        System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
                    }
                    
                } catch (SocketTimeoutException e) {
                    System.out.println(" Timeout! Il server non risponde.");
                    System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
                } catch (Exception e) {
                    System.out.println(" Errore di comunicazione!");
                    System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
                }
            }
        }
    }

    public static void main(String[] args) {

        Scanner s = new Scanner(System.in);
        ClearScreen screen = new ClearScreen();

        try (
            Socket socket = new Socket("localhost", 5000);
            ObjectOutputStream outObj = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inObj = new ObjectInputStream(socket.getInputStream());
        ) {

            socket.setSoTimeout(5000); 
            
            int clientId = (int) inObj.readObject();

            int option;

            do {
                screen.clear();
                System.out.println("----- CLIENT " + clientId + " -----");
                System.out.println("\n1 - Inserisci un'operazione");
                System.out.println("2 - Visualizza le operazioni");
                System.out.println("3 - Invia le operazioni");
                System.out.println("4 - Visualizza risultati");
                System.out.println("5 - Chat condivisa");
                System.out.println("0 - Esci");

                System.out.print("\nInserisci: ");
                option = s.nextInt();
                s.nextLine();

                switch(option) {

                    case 1:
                        inserisci(s, screen);
                        break;

                    case 2:
                        screen.clear();
                        System.out.println("----- OPERAZIONI MEMORIZZATE -----");

                        if(listaOperazioni.isEmpty()) {
                            System.out.println("Nessuna operazione inserita.");
                        } else {
                            for(Operazione op : listaOperazioni) {
                                System.out.println(op.toString());
                            }
                        }

                        System.out.println("\nPremi invio per tornare al menu...");
                        s.nextLine();
                        break;

                    case 3:
                        if(listaOperazioni.isEmpty()) {
                            System.out.println("Nessuna operazione da inviare.\n\nPremi INVIO per continuare.");
                            s.nextLine();
                        } else {
                            screen.clear();
                            System.out.println("Operazioni inviate correttamente al server.");

                            // Imposto l'indice della prima operazione
                            primaOperazione = ultimaOperazione;

                            // Invio le operazioni al server
                            for(Operazione op : listaOperazioni) {

                                // Invia operazione
                                outObj.writeObject(op);
                                outObj.flush();

                                // Riceve risultato
                                String risultato = (String) inObj.readObject();

                                // Aggiungo il risultato alla lista
                                listaRisultati.add(risultato);
                            }

                            ultimaOperazione = listaRisultati.size(); // Imposto l'indice dell'ultima operazione

                            listaOperazioni.clear();

                            System.out.println("\n\nPremi INVIO per continuare...");
                            s.nextLine();
                        }
                        break;
                    case 4:
                        screen.clear();

                        if(listaRisultati.isEmpty()) {
                            System.out.println("Nessun risultato disponibile.\n\nPremi INVIO per continuare.");
                            s.nextLine();
                        } else {
                            System.out.println("----- RISULTATI MEMORIZZATI -----");
                            if(primaOperazione != 0) {
                                for(int i = 0; i < primaOperazione; i++) {
                                    System.out.println(listaRisultati.get(i)); // Se la lista è vuota non entra nel ciclo (listaRisultati.isEmpty())
                                }
                            } else {
                                System.out.println("Nulla da mostrare.");
                            }

                            System.out.println("\n\n----- ULTIMI RISULTATI -----");
                            for(int i = primaOperazione; i < listaRisultati.size(); i++) { // Stampo solo gli ultimi risultati salvati
                                System.out.println(listaRisultati.get(i)); // Se la lista è vuota non entra nel ciclo (listaRisultati.isEmpty())
                            }
                        }

                        System.out.println("\n\nPremi INVIO per continuare...");
                        s.nextLine();
                        break;
                    case 5:
                        screen.clear();
                        chat(s, screen, inObj, outObj, clientId);
                        break;
                    case 0:
                        System.out.println("Uscita...");
                        break;

                    default:
                        System.out.println("Opzione non valida.");
                        break;
                }

            } while(option != 0);

            screen.clear();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
