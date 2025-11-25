import java.io.*;                 // Importa classi per input/output (stream, reader, writer)
import java.net.*;                // Importa classi per la rete: Socket
import java.util.ArrayList;       // Importa la struttura dati ArrayList
import java.util.Scanner;         // Importa Scanner per input da tastiera

public class Client {

    // Lista globale che contiene tutte le operazioni inserite dall'utente
    static ArrayList<Operazione> listaOperazioni = new ArrayList<>();

    // ----- METODO PER INSERIRE UNA NUOVA OPERAZIONE -----
    public static void inserisci(Scanner s, ClearScreen screen) {
        int option;

        do {
            screen.clear();  // Pulizia della console
            System.out.println("----- INSERISCI OPERAZIONE -----");
            System.out.println("\n1 - Somma");
            System.out.println("2 - Sottrazione");
            System.out.println("3 - Moltiplicazione");
            System.out.println("4 - Divisione");
            System.out.println("5 - Esci");

            System.out.print("\nInserisci: ");
            option = s.nextInt();   // Leggi la scelta utente
            s.nextLine();           // Pulizia buffer

            // Se l'opzione selezionata è valida (1-4)
            if(option >= 1 && option <= 4) {

                // Chiede i due numeri dell'operazione
                System.out.print("Primo numero: ");
                int a = s.nextInt();
                System.out.print("Secondo numero: ");
                int b = s.nextInt();
                s.nextLine();  // Pulizia buffer

                // Associa l'operatore in base all'opzione scelta
                String tipo = "";
                switch(option) {
                    case 1: tipo = "+"; break;
                    case 2: tipo = "-"; break;
                    case 3: tipo = "*"; break;
                    case 4: tipo = "/"; break;
                }

                // Crea l'oggetto Operazione e lo aggiunge alla lista
                listaOperazioni.add(new Operazione(tipo, a, b));
                System.out.println("Operazione aggiunta: " + a + " " + tipo + " " + b);
                
                // Aspetta input per continuare
                System.out.println("\nPremi invio per continuare...");
                s.nextLine();

            } else if(option == 5) {
                // Torna al menu principale
                System.out.println("Ritorno al menu principale...");
            } else {
                // Scelta non valida
                System.out.println("Opzione non valida.");
            }

        } while(option != 5);  // Ripete fino a quando l’utente sceglie "Esci"
    }

    // ----- METODO PRINCIPALE -----
    public static void main(String[] args) {

        Scanner s = new Scanner(System.in);         // Input da tastiera
        ClearScreen screen = new ClearScreen();     // Oggetto per pulire la console

        // Creazione della socket per connettersi al server sulla porta 5000
        try (Socket socket = new Socket("localhost", 5000)) {

            // Writer per inviare dati al server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Reader per leggere risposte dal server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            int option;

            do {
                screen.clear();
                System.out.println("----- MENU -----");
                System.out.println("\n1 - Inserisci un'operazione");
                System.out.println("2 - Visualizza le operazioni");
                System.out.println("3 - Invia le operazioni");
                System.out.println("4 - Esci");

                System.out.print("\nInserisci: ");
                option = s.nextInt();   // Legge l’opzione
                s.nextLine();           // Pulizia buffer

                switch(option) {

                    case 1:
                        // Menu per inserire nuove operazioni
                        inserisci(s, screen);
                        break;

                    case 2:
                        // Visualizza tutte le operazioni memorizzate
                        screen.clear();
                        System.out.println("----- OPERAZIONI MEMORIZZATE -----");

                        if(listaOperazioni.isEmpty()) {
                            System.out.println("Nessuna operazione inserita.");
                        } else {
                            // Stampa ogni operazione con indice
                            for(int i = 0; i < listaOperazioni.size(); i++) {
                                System.out.println((i+1) + ": " + listaOperazioni.get(i));
                            }
                        }

                        System.out.println("\nPremi invio per tornare al menu...");
                        s.nextLine();
                        break;

                    case 3:
                        // Invia ogni operazione in un thread separato
                        if(listaOperazioni.isEmpty()) {
                            System.out.println("Nessuna operazione da inviare.\n\nPremi INVIO per continuare.");
                            s.nextLine();
                        } else {
                            // Per ogni operazione, crea e avvia un OperazioneThread
                            screen.clear();
                            ArrayList<OperazioneThread> threads = new ArrayList<>();
                            System.out.println("----- RISULTATI OTTENUTI DAL SERVER -----");
                            for(Operazione op : listaOperazioni) {
                                OperazioneThread t = new OperazioneThread(op, "localhost", 5000);
                                threads.add(t);
                                t.start();   // Avvio del thread
                            }

                            // Aspetto che tutti i thread finiscano la propria esecuzione
                            for(OperazioneThread t : threads) {
                                try {
                                    t.join(); // aspetta che il thread finisca
                                } catch(InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            listaOperazioni.clear();
                            
                            System.out.println("\n\nPremi INVIO per continuare...");
                            s.nextLine();
                        }
                        break;

                    case 4:
                        // Esci dal programma
                        System.out.println("Uscita...");
                        break;

                    default:
                        System.out.println("Opzione non valida.");
                        break;
                }

            } while(option != 4);   // Ripete finché l’utente non sceglie "Esci"

            screen.clear();

        } catch(Exception e) {
            // Gestione errori generali (connessione, I/O, ecc.)
            e.printStackTrace();
        }
    }
}
