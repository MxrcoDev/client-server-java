import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Client {

    // Coda locale delle operazioni inserite dall'utente (non condivisa → no concorrenza)
    static Queue<Operazione> listaOperazioni = new LinkedList<>();

    // Memorizza tutti i risultati ricevuti dal server
    static List<String> listaRisultati = new ArrayList<>();

    // Indici per distinguere i risultati “vecchi” da quelli dell’ultimo invio
    static int primaOperazione = 0;
    static int ultimaOperazione = 0;

    // Copia locale della chat condivisa sul server
    static Queue<Messaggio> listaMessaggi = new LinkedList<>();

    // Menu per inserire le operazioni matematiche
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

                // Lettura dei due numeri da inviare al server
                System.out.print("Primo numero: ");
                int a = s.nextInt();
                System.out.print("Secondo numero: ");
                int b = s.nextInt();
                s.nextLine();

                // Determina il tipo di operazione
                String tipo = "";
                switch(option) {
                    case 1: tipo = "+"; break;
                    case 2: tipo = "-"; break;
                    case 3: tipo = "*"; break;
                    case 4: tipo = "/"; break;
                }

                // Aggiunge l’operazione alla coda locale
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

    // Richiede al server la coda dei messaggi della chat e la stampa
    public static void visualizzaMessaggi(ClearScreen screen, ObjectInputStream inObj, ObjectOutputStream outObj) {

        try {
            // Richiesta esplicita della coda dei messaggi (operazione di lettura condivisa)
            outObj.writeObject("RICHIESTA_CODA"); 
            outObj.flush();

            // Riceve la copia sincronizzata della coda dal server
            Object obj = inObj.readObject();
            if(obj instanceof Queue) {
                Queue<Messaggio> coda = (Queue<Messaggio>) obj;

                // Aggiorna la copia locale
                listaMessaggi.clear();
                listaMessaggi.addAll(coda);
            }
        } catch (Exception e) {}

        // Stampa della chat
        screen.clear();
        System.out.println("----- CHAT -----");
        if (!listaMessaggi.isEmpty()) {
            for(Messaggio m : listaMessaggi) {
                System.out.println(m.get());
            }
        }

        System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
    }
    
    // Gestione della chat lato client
    public static void chat(Scanner s, ClearScreen screen, ObjectInputStream inObj, ObjectOutputStream outObj, int clientId) {
        boolean exitChat = false;
        
        // Mostra subito la chat aggiornata all’ingresso
        visualizzaMessaggi(screen, inObj, outObj);
    
        while(!exitChat) {
            String text = s.nextLine();
    
            if(text.equals("exit")) {
                exitChat = true;

            } else if (text.equals("1")) {
                // Aggiorna la chat richiedendo la coda al server
                visualizzaMessaggi(screen, inObj, outObj);

            } else if(!text.trim().isEmpty()) {
                
                System.out.print("\nInvio in corso...");
                
                try {
                    // Invia un nuovo messaggio serializzato al server
                    outObj.writeObject(new Messaggio(clientId, text));
                    outObj.flush();
                    
                    // Attende una conferma (evita invii doppi se ci sono ritardi)
                    Object conferma = inObj.readObject();
                    
                    if(conferma instanceof String && conferma.equals("MSG_OK")) {
                        // Se tutto ok, aggiorna la chat
                        visualizzaMessaggi(screen, inObj, outObj);
                    } else {
                        System.out.println(" Errore!");
                        System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");
                    }
                    
                } catch (SocketTimeoutException e) {
                    // Protegge da eventuali blocchi se il server non risponde
                    System.out.println(" Timeout! Il server non risponde.");
                    System.out.print("\n\n'exit' per uscire | '1' per aggiornare >> ");

                } catch (Exception e) {
                    // Errore generico di comunicazione
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
            // Il client apre una socket verso il server
            Socket socket = new Socket("localhost", 5000);

            // Stream di uscita e ingresso di oggetti serializzati
            ObjectOutputStream outObj = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inObj = new ObjectInputStream(socket.getInputStream());
        ) {

            // Timeout per evitare blocchi su readObject() in caso di server lento o disconnesso
            socket.setSoTimeout(5000); 
            
            // Il server assegna un ID univoco a questo client
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
                        // Permette di creare nuove operazioni
                        inserisci(s, screen);
                        break;

                    case 2:
                        // Stampa le operazioni non ancora inviate
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
                        // Invio delle operazioni al server
                        if(listaOperazioni.isEmpty()) {
                            System.out.println("Nessuna operazione da inviare.\n\nPremi INVIO per continuare.");
                            s.nextLine();
                        } else {
                            screen.clear();
                            System.out.println("Operazioni inviate correttamente al server.");

                            // Salva l'indice dal quale iniziano i nuovi risultati
                            primaOperazione = ultimaOperazione;

                            // Invio di ogni singola operazione e ricezione risultato
                            for(Operazione op : listaOperazioni) {

                                outObj.writeObject(op);
                                outObj.flush();

                                // Il server risponde con una stringa risultato
                                String risultato = (String) inObj.readObject();

                                listaRisultati.add(risultato);
                            }

                            // Aggiorna l’indice dell’ultimo risultato disponibile
                            ultimaOperazione = listaRisultati.size();

                            // Svuota la lista delle operazioni locali
                            listaOperazioni.clear();

                            System.out.println("\n\nPremi INVIO per continuare...");
                            s.nextLine();
                        }
                        break;

                    case 4:
                        // Sezione per visualizzare i risultati delle operazioni
                        screen.clear();

                        if(listaRisultati.isEmpty()) {
                            System.out.println("Nessun risultato disponibile.\n\nPremi INVIO per continuare.");
                            s.nextLine();
                        } else {
                            System.out.println("----- RISULTATI MEMORIZZATI -----");

                            // Mostra i risultati precedenti
                            if(primaOperazione != 0) {
                                for(int i = 0; i < primaOperazione; i++) {
                                    System.out.println(listaRisultati.get(i));
                                }
                            } else {
                                System.out.println("Nulla da mostrare.");
                            }

                            // Mostra gli ultimi risultati
                            System.out.println("\n\n----- ULTIMI RISULTATI -----");
                            for(int i = primaOperazione; i < listaRisultati.size(); i++) {
                                System.out.println(listaRisultati.get(i));
                            }
                        }

                        System.out.println("\n\nPremi INVIO per continuare...");
                        s.nextLine();
                        break;

                    case 5:
                        // Entra nella chat condivisa
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
