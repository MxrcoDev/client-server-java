import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.*;

public class Client {

    private static CardLayout cardLayout;
    private static JPanel mainPanel;

    // Variabili per gestire le operazioni
    static Queue<Operazione> listaOperazioni = new LinkedList<>();
    static java.util.List<String> listaRisultati = new ArrayList<>();
    // Variabili per indicare l'indice della prima e dell'ultima operazione di un burst di invio
    static int primaOperazione = 0;
    static int ultimaOperazione = 0;

    // Variabili per la chat
    static Queue<Messaggio> listaMessaggi = new LinkedList<>();
    public static boolean onChat = false;
    private static volatile Object ultimaRisposta = null;
    private static final Object lockRisposta = new Object();

    // Stream globali per mantenere la connessione aperta
    private static ObjectOutputStream outObj;
    private static ObjectInputStream inObj;
    private static Socket socket;

    // Palette colori
    private static final Color BG_DARK = new Color(18, 18, 18);
    private static final Color PANEL_DARK = new Color(28, 28, 30);
    private static final Color BTN_DARK = new Color(48, 48, 52);
    private static final Color BTN_HOVER = new Color(68, 68, 72);
    private static final Color BTN_ACCENT = new Color(0, 122, 255);
    private static final Color BTN_ACCENT_HOVER = new Color(10, 132, 255);
    private static final Color TEXT_LIGHT = new Color(245, 245, 247);
    private static final Color TEXT_SECONDARY = new Color(152, 152, 157);
    private static final Color DISPLAY_BG = new Color(24, 24, 26);
    private static final Color BORDER_SUBTLE = new Color(58, 58, 60);

    // Font system
    private static final Font MODERN = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font MODERN_BOLD = new Font("Segoe UI", Font.BOLD, 16);

    // ------ BOTTONI CUSTOM MODERNI ------
    private static JButton modernButton(String text) {
        return modernButton(text, false);
    }

    private static JButton modernButton(String text, boolean isAccent) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                
                // Ombra sottile
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 12, 12);
                
                // Sfondo bottone
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };

        b.setFont(MODERN);
        b.setBackground(isAccent ? BTN_ACCENT : BTN_DARK);
        b.setForeground(TEXT_LIGHT);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover con transizione
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                b.setBackground(isAccent ? BTN_ACCENT_HOVER : BTN_HOVER); 
            }
            public void mouseExited(MouseEvent e) { 
                b.setBackground(isAccent ? BTN_ACCENT : BTN_DARK); 
            }
        });

        return b;
    }

    // ------------------------------------------------------
    //   SCHERMATA CHAT
    // ------------------------------------------------------
    private static JPanel chatMessagesPanel;
    private static JScrollPane chatScrollPane;
    private static int localClientId = 0;

    public static JPanel chatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Titolo
        JLabel title = new JLabel("Chat Condivisa - Client " + localClientId, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(title, BorderLayout.NORTH);

        // Area messaggi
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setBackground(DISPLAY_BG);
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatScrollPane = new JScrollPane(chatMessagesPanel);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.getViewport().setBackground(DISPLAY_BG);
        panel.add(chatScrollPane, BorderLayout.CENTER);

        // Pannello inferiore (input + bottoni)
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(BG_DARK);

        JTextField inputField = new JTextField();
        inputField.setFont(MODERN);
        inputField.setBackground(DISPLAY_BG);
        inputField.setForeground(TEXT_LIGHT);
        inputField.setCaretColor(TEXT_LIGHT);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonsPanel.setBackground(BG_DARK);

        JButton sendBtn = modernButton("Invia", true);
        JButton backBtn = modernButton("Menu");

        buttonsPanel.add(backBtn);
        buttonsPanel.add(sendBtn);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Listener per inviare messaggio
        ActionListener sendAction = e -> {
            String testo = inputField.getText().trim();
            if (!testo.isEmpty()) {
                inviaMessaggio(testo);
                inputField.setText("");
            }
        };

        sendBtn.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        // Listener per tornare al menu
        backBtn.addActionListener(e -> {
            onChat = false;
            listaMessaggi.clear();
            cardLayout.show(mainPanel, "menu");
        });

        return panel;
    }

    public static void aggiornaChat() {
        SwingUtilities.invokeLater(() -> {
            chatMessagesPanel.removeAll();

            for (Messaggio msg : listaMessaggi) {
                JPanel msgPanel = new JPanel();
                msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
                msgPanel.setBackground(DISPLAY_BG);
                msgPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

                boolean isMine = (msg.getMittente() == localClientId);

                JLabel msgLabel = new JLabel("<html><div style='padding:8px;'>" +
                    "<b style='color:" + (isMine ? "#0a84ff" : "#ff9f0a") + ";'>" +
                    "Client " + msg.getMittente() + "</b><br>" +
                    "<span style='color:#f5f5f7;'>" + msg.getTesto() + "</span>" +
                    "</div></html>");
                
                msgLabel.setOpaque(true);
                msgLabel.setBackground(isMine ? new Color(38, 38, 42) : new Color(48, 48, 52));
                msgLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));

                msgPanel.add(msgLabel);
                msgPanel.setAlignmentX(isMine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
                
                chatMessagesPanel.add(msgPanel);
            }

            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();

            // Scroll automatico in fondo
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private static void inviaMessaggio(String testo) {
        new Thread(() -> {
            try {
                Messaggio msg = new Messaggio(localClientId, testo);
                
                outObj.writeObject(msg);
                outObj.flush();
                
                // Attendi conferma usando il sistema di risposte
                Object risposta = attendiRisposta();
                if (risposta instanceof String && risposta.equals("MSG_OK")) {
                    // Aggiungi il messaggio anche localmente
                    listaMessaggi.add(msg);
                    aggiornaChat();
                } else {
                    System.err.println("Conferma non valida: " + risposta);
                }
            } catch (Exception e) {
                System.err.println("Errore durante invio messaggio: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private static void richiediCoda() {
        new Thread(() -> {
            try {
                outObj.writeObject("RICHIESTA_CODA");
                outObj.flush();
                
                Object risposta = attendiRisposta();
                
                if (risposta instanceof Queue) {
                    @SuppressWarnings("unchecked")
                    Queue<Messaggio> codaRicevuta = (Queue<Messaggio>) risposta;
                    
                    listaMessaggi.clear();
                    listaMessaggi.addAll(codaRicevuta);
                    aggiornaChat();
                } else {
                    System.err.println("Risposta coda non valida: " + (risposta != null ? risposta.getClass() : "null"));
                }
            } catch (Exception e) {
                System.err.println("Errore richiesta coda: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Sistema di attesa risposta sincronizzato
    private static Object attendiRisposta() throws InterruptedException {
        synchronized(lockRisposta) {
            while (ultimaRisposta == null) {
                lockRisposta.wait(5000); // Timeout di 5 secondi
                if (ultimaRisposta == null) {
                    throw new RuntimeException("Timeout attesa risposta");
                }
            }
            Object risposta = ultimaRisposta;
            ultimaRisposta = null;
            return risposta;
        }
    }

    // Thread di ricezione GLOBALE che smista tutti i messaggi
    private static void avviaThreadRicezione() {
        Thread receiverThread = new Thread(() -> {
            System.out.println("Thread di ricezione avviato per Client " + localClientId);
            
            while (true) {
                try {
                    Object obj = inObj.readObject();
                    
                    if (obj instanceof Messaggio) {
                        // Messaggio broadcast - aggiorna la chat se siamo dentro
                        Messaggio msg = (Messaggio) obj;
                        if (onChat) {
                            listaMessaggi.add(msg);
                            aggiornaChat();
                            System.out.println("Broadcast ricevuto: " + msg.get());
                        }
                    } else {
                        // Risposta a una richiesta (String, Queue, ecc.)
                        synchronized(lockRisposta) {
                            ultimaRisposta = obj;
                            lockRisposta.notify();
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Errore nel thread di ricezione: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
            
            System.out.println("Thread di ricezione terminato");
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    // ------------------------------------------------------
    //   SCHERMATA CALCOLATRICE
    // ------------------------------------------------------
    public static JPanel calcolatricePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("", SwingConstants.RIGHT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        label.setOpaque(true);
        label.setBackground(DISPLAY_BG);
        label.setForeground(TEXT_LIGHT);
        label.setPreferredSize(new Dimension(400, 80));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        panel.add(label, BorderLayout.NORTH);

        JPanel pannello = new JPanel(new GridLayout(4, 4, 10, 10));
        pannello.setBackground(BG_DARK);
        String chars = "123+456-789*.0</";
        int idx = 0;
        final String[] op = {""};

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {

                char c = chars.charAt(idx);
                JButton btn = modernButton("" + c);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 20));

                if (c == '+' || c == '-' || c == '*' || c == '/') {
                    btn.setForeground(BTN_ACCENT);
                }

                switch (c) {
                    case '<':
                        btn.setText("<");
                        btn.setFont(new Font("Segoe UI", Font.PLAIN, 22));
                        btn.addActionListener(e -> {
                            String t = label.getText();
                            if (!t.isEmpty()) {
                                if (t.endsWith(" ")) {
                                    label.setText(t.substring(0, t.length() - 3));
                                    op[0] = "";
                                } else {
                                    label.setText(t.substring(0, t.length() - 1));
                                }
                            }
                        });
                        break;

                    case '+': case '-': case '*': case '/':
                        btn.addActionListener(e -> {
                            if (op[0].equals("") && !label.getText().isEmpty()) {
                                label.setText(label.getText() + " " + c + " ");
                                op[0] = "" + c;
                            }
                        });
                        break;

                    default:
                        btn.addActionListener(e -> label.setText(label.getText() + c));
                        break;
                }

                pannello.add(btn);
                idx++;
            }
        }

        panel.add(pannello, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setBackground(BG_DARK);

        JButton back = modernButton("Menu");
        back.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JButton equals = modernButton("=", true);
        equals.setFont(new Font("Segoe UI", Font.BOLD, 26));

        equals.addActionListener(e -> {
            try {
                String t = label.getText();
                if (!op[0].isEmpty()) {
                    double a = Double.parseDouble(t.substring(0, t.indexOf(" ")));
                    double b = Double.parseDouble(t.substring(t.lastIndexOf(" ") + 1));
                    Operazione operazione = new Operazione(op[0], a, b);

                    listaOperazioni.add(operazione);
                    // System.out.println("Operazione aggiunta: " + operazione);

                    label.setText("");
                    op[0] = "";
                }
            } catch (Exception ex) {
                // System.out.println("Errore nell'operazione: " + ex.getMessage());
            }
        });

        bottom.add(back);
        bottom.add(equals);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ------------------------------------------------------
    //   VISUALIZZA OPERAZIONI
    // ------------------------------------------------------
    private static JPanel nuovaPagina;
    private static JPanel listPanel;
    
    public static JPanel listaOperazioni() {
        nuovaPagina = new JPanel(new BorderLayout());
        nuovaPagina.setBackground(BG_DARK);
        nuovaPagina.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
        JLabel title = new JLabel("Operazioni in coda", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        nuovaPagina.add(title, BorderLayout.NORTH);
    
        listPanel = new JPanel();
        listPanel.setBackground(BG_DARK);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_DARK);
        nuovaPagina.add(scroll, BorderLayout.CENTER);
    
        JPanel bottom = new JPanel();
        bottom.setBackground(BG_DARK);
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    
        JButton back = modernButton("Torna al menu");
        back.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        bottom.add(back);
    
        nuovaPagina.add(bottom, BorderLayout.SOUTH);
    
        return nuovaPagina;
    }
    
    public static void aggiornaListaOperazioni() {
        listPanel.removeAll();
    
        if (listaOperazioni.isEmpty()) {
            JLabel emptyLabel = new JLabel("Nessuna operazione in coda.");
            emptyLabel.setForeground(TEXT_SECONDARY);
            emptyLabel.setFont(MODERN);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            listPanel.add(emptyLabel);
        } else {
            for (Operazione o : listaOperazioni) {
                JLabel label = new JLabel(o.toString());
                label.setForeground(TEXT_LIGHT);
                label.setFont(MODERN);
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                listPanel.add(label);
            }
        }
    
        listPanel.revalidate();
        listPanel.repaint();
    }

    // ------------------------------------------------------
    //   INVIA OPERAZIONI
    // ------------------------------------------------------
    public static void inviaOperazioni() {
        if(!listaOperazioni.isEmpty()) {
            primaOperazione = ultimaOperazione;
    
            int count = 0;
            for(Operazione op : listaOperazioni) {
                try {
                    count++;
                    
                    outObj.writeObject(op);
                    outObj.flush();
    
                    // Usa il nuovo sistema di attesa risposta
                    Object risposta = attendiRisposta();
                    if (risposta instanceof String) {
                        String risultato = (String) risposta;
                        listaRisultati.add(risultato);
                    } else {
                        System.err.println("Risposta operazione non valida: " + risposta);
                    }
                } catch(Exception e) {
                    System.err.println("ERRORE durante operazione #" + count + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
    
            ultimaOperazione = listaRisultati.size();
            listaOperazioni.clear();
        }
    }

    // ------------------------------------------------------
    //   VISUALIZZA RISULTATI
    // ------------------------------------------------------
    private static JPanel risultatiPagina;
    private static JPanel resultsPanel;
    
    public static JPanel visualizzaRisultati() {
        risultatiPagina = new JPanel(new BorderLayout());
        risultatiPagina.setBackground(BG_DARK);
        risultatiPagina.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
        // Titolo
        JLabel title = new JLabel("Risultati Operazioni", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        risultatiPagina.add(title, BorderLayout.NORTH);
    
        // Pannello scrollabile per i risultati
        resultsPanel = new JPanel();
        resultsPanel.setBackground(BG_DARK);
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
    
        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_DARK);
        risultatiPagina.add(scroll, BorderLayout.CENTER);
    
        // Pannello inferiore con bottone per tornare al menu
        JPanel bottom = new JPanel();
        bottom.setBackground(BG_DARK);
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    
        JButton back = modernButton("Torna al menu");
        back.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        bottom.add(back);
    
        risultatiPagina.add(bottom, BorderLayout.SOUTH);
    
        return risultatiPagina;
    }
    
    public static void aggiornaListaRisultati() {
        resultsPanel.removeAll();
        
        // Popola i risultati
        if (listaRisultati.isEmpty()) {
            JLabel emptyLabel = new JLabel("Nessun risultato disponibile.");
            emptyLabel.setForeground(TEXT_SECONDARY);
            emptyLabel.setFont(MODERN);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            resultsPanel.add(emptyLabel);
        } else {
            // Risultati storici (se esistono)
            if (primaOperazione > 0) {
                JLabel storicoLabel = new JLabel("----- RISULTATI STORICI -----");
                storicoLabel.setForeground(BTN_ACCENT);
                storicoLabel.setFont(MODERN_BOLD);
                storicoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
                resultsPanel.add(storicoLabel);
    
                for (int i = 0; i < primaOperazione; i++) {
                    JLabel label = new JLabel(listaRisultati.get(i));
                    label.setForeground(TEXT_LIGHT);
                    label.setFont(MODERN);
                    label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                    resultsPanel.add(label);
                }
            }
    
            // Ultimi risultati (se esistono)
            if (primaOperazione < listaRisultati.size()) {
                JLabel ultimiLabel = new JLabel("----- ULTIMI RISULTATI -----");
                ultimiLabel.setForeground(BTN_ACCENT);
                ultimiLabel.setFont(MODERN_BOLD);
                ultimiLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
                resultsPanel.add(ultimiLabel);
        
                for (int i = primaOperazione; i < listaRisultati.size(); i++) {
                    JLabel label = new JLabel(listaRisultati.get(i));
                    label.setForeground(TEXT_LIGHT);
                    label.setFont(MODERN);
                    label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                    resultsPanel.add(label);
                }
            }
        }
        
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    // ------------------------------------------------------
    //   MENU PRINCIPALE
    // ------------------------------------------------------
    public static JPanel menuPanel(int id) {

        localClientId = id;

        JPanel menu = new JPanel(new BorderLayout());
        menu.setBackground(BG_DARK);

        JLabel titleLabel = new JLabel("Client " + id, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 30, 0));
        menu.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(6, 1, 0, 12));
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 60, 50));

        JButton op1 = modernButton("Aggiungi operazione");
        JButton op2 = modernButton("Visualizza operazioni");
        JButton op3 = modernButton("Invia operazioni");
        JButton op4 = modernButton("Visualizza risultati");
        JButton op5 = modernButton("Chat condivisa");
        JButton op6 = modernButton("Chiudi client");

        op1.setFont(MODERN_BOLD);
        op2.setFont(MODERN_BOLD);
        op3.setFont(MODERN_BOLD);
        op4.setFont(MODERN_BOLD);
        op5.setFont(MODERN_BOLD);
        op6.setFont(MODERN_BOLD);

        centerPanel.add(op1);
        centerPanel.add(op2);
        centerPanel.add(op3);
        centerPanel.add(op4);
        centerPanel.add(op5);
        centerPanel.add(op6);

        menu.add(centerPanel, BorderLayout.CENTER);

        op1.addActionListener(e -> cardLayout.show(mainPanel, "calc"));
        
        op2.addActionListener(e -> {
            aggiornaListaOperazioni();
            cardLayout.show(mainPanel, "nuova");
        });

        op3.addActionListener(e -> {
            Color originalColor = op3.getBackground();
            op3.setBackground(new Color(144, 238, 144));
            op3.repaint();
        
            new javax.swing.Timer(2000, evt -> {
                op3.setBackground(originalColor);
                op3.repaint();
            }).start();

            // Esegui l'invio in un thread separato per non bloccare la GUI
            new Thread(() -> inviaOperazioni()).start();
        });

        op4.addActionListener(e -> {
            aggiornaListaRisultati();
            cardLayout.show(mainPanel, "risultati");
        });

        // Nella parte con op5.addActionListener
        op5.addActionListener(e -> {
            onChat = true;
            richiediCoda();
            cardLayout.show(mainPanel, "chat");
        });

        op6.addActionListener(e -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        return menu;
    }

    // ------------------------------------------------------
    //   HEADER PERSONALIZZATO
    // ------------------------------------------------------
    private static JPanel intestazione(JFrame frame) {
        JPanel header = new JPanel(null); 
        header.setBackground(new Color(28, 28, 30));
        header.setPreferredSize(new Dimension(800, 44));
        
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE));
    
        JLabel title = new JLabel("Client Calcolatrice", SwingConstants.CENTER);
        title.setForeground(TEXT_LIGHT);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setBounds(0, 0, 800, 44);
    
        JButton close = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        close.setBounds(16, 14, 16, 16);
        close.setBackground(new Color(255, 95, 86));
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setContentAreaFilled(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });
    
        JButton minimize = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        minimize.setBounds(40, 14, 16, 16);
        minimize.setBackground(new Color(255, 189, 46));
        minimize.setBorderPainted(false);
        minimize.setFocusPainted(false);
        minimize.setContentAreaFilled(false);
        minimize.setCursor(new Cursor(Cursor.HAND_CURSOR));
        minimize.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
    
        header.add(title);
        header.add(close);
        header.add(minimize);
    
        final Point clickPoint = new Point();
        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                clickPoint.x = e.getX();
                clickPoint.y = e.getY();
            }
        });
        header.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen() - clickPoint.x;
                int y = e.getYOnScreen() - clickPoint.y;
                frame.setLocation(x, y);
            }
        });
    
        return header;
    }

    // ------------------------------------------------------
    //                      MAIN
    // ------------------------------------------------------
    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 5000);
            outObj = new ObjectOutputStream(socket.getOutputStream());
            inObj = new ObjectInputStream(socket.getInputStream());
    
            int clientId = (Integer) inObj.readObject();
            localClientId = clientId;
            System.out.println("Connesso al server con ID: " + clientId);
            
            // Avvia il thread di ricezione GLOBALE
            avviaThreadRicezione();
            
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame();
                frame.setSize(470, 600);
                frame.setUndecorated(true);
                frame.setResizable(false);
                frame.setLayout(new BorderLayout());
    
                cardLayout = new CardLayout();
                mainPanel = new JPanel(cardLayout);
    
                mainPanel.add(menuPanel(clientId), "menu");
                mainPanel.add(calcolatricePanel(), "calc");
                mainPanel.add(listaOperazioni(), "nuova");
                mainPanel.add(visualizzaRisultati(), "risultati");
                mainPanel.add(chatPanel(), "chat");
    
                frame.add(intestazione(frame), BorderLayout.NORTH);
                frame.add(mainPanel, BorderLayout.CENTER);
    
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
            
        } catch (Exception e) {
            System.err.println("Errore di connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }
}