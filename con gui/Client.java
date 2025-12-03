import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Queue;
import java.util.LinkedList;

public class Client {

    private static CardLayout cardLayout;
    private static JPanel mainPanel;

    private static Queue<Operazione> listaOperazioni = new LinkedList<>();

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
    //   VISUALIZZA OPERAZIONI
    // ------------------------------------------------------

    private static JPanel nuovaPagina;        // La card
    private static JPanel listPanel;          // Contenitore delle operazioni
    
    public static JPanel nuovaPaginaPanel() {
        nuovaPagina = new JPanel(new BorderLayout());
        nuovaPagina.setBackground(BG_DARK);
        nuovaPagina.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
        // Titolo pagina
        JLabel title = new JLabel("Operazioni eseguite", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_LIGHT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        nuovaPagina.add(title, BorderLayout.NORTH);
    
        // Pannello interno per lista operazioni
        listPanel = new JPanel();
        listPanel.setBackground(BG_DARK);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    
        // Scroll pane elegante
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_DARK);
        nuovaPagina.add(scroll, BorderLayout.CENTER);
    
        // Pannello inferiore con pulsante per tornare al menu
        JPanel bottom = new JPanel();
        bottom.setBackground(BG_DARK);
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
    
        JButton back = modernButton("← Torna al menu");
        back.addActionListener(e -> cardLayout.show(mainPanel, "menu"));
        bottom.add(back);
    
        nuovaPagina.add(bottom, BorderLayout.SOUTH);
    
        return nuovaPagina;
    }
    
    // Richiamare questa funzione **prima di mostrare la pagina** per aggiornare le operazioni
    public static void aggiornaListaOperazioni() {
        listPanel.removeAll(); // cancella vecchi JLabel
    
        for (Operazione o : listaOperazioni) {
            JLabel label = new JLabel(o.toString());
            label.setForeground(TEXT_LIGHT);
            label.setFont(MODERN);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            listPanel.add(label);
        }
    
        listPanel.revalidate();
        listPanel.repaint();
    }
    
    

    
    // ------------------------------------------------------
    //   SCHERMATA CALCOLATRICE
    // ------------------------------------------------------
    public static JPanel calcolatricePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Display elegante con bordo
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

        // Griglia bottoni con spaziatura maggiore
        JPanel pannello = new JPanel(new GridLayout(4, 4, 10, 10));
        pannello.setBackground(BG_DARK);
        String chars = "123+456-789*.0</";
        int idx = 0;
        final String[] op = {""};

        // Creazione dei bottoni
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

        // Pannello inferiore con bottoni eleganti
        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setBackground(BG_DARK);

        JButton back = modernButton("← Menu");
        back.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JButton equals = modernButton("=", true);
        equals.setFont(new Font("Segoe UI", Font.BOLD, 26));

        equals.addActionListener(e -> {
            try {
                String t = label.getText();
                if (!op[0].isEmpty()) {
                    double a = Double.parseDouble(t.substring(0, t.indexOf(" ")));
                    double b = Double.parseDouble(t.substring(t.lastIndexOf(" ") + 1));
                    Operazione operazione = new Operazione(a, b, op[0]);

                    listaOperazioni.add(operazione);

                    // Reimposta il label a vuoto per accettare una nuova operazione
                    label.setText("");
                    op[0] = "";
                }
            } catch (Exception ex) {
                System.out.println("Errore nell'operazione");
            }
        });

        bottom.add(back);
        bottom.add(equals);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ------------------------------------------------------
    //   MENU PRINCIPALE
    // ------------------------------------------------------
    public static JPanel menuPanel() {

        JPanel menu = new JPanel(new BorderLayout());
        menu.setBackground(BG_DARK);

        // Titolo elegante
        JLabel titleLabel = new JLabel("Calcolatrice", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 30, 0));
        menu.add(titleLabel, BorderLayout.NORTH);

        // Pannello centrale con bottoni
        JPanel centerPanel = new JPanel(new GridLayout(4, 1, 0, 12));
        centerPanel.setBackground(BG_DARK);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 60, 50));

        JButton op1 = modernButton("Aggiungi operazione");
        JButton op2 = modernButton("Visualizza operazioni");
        JButton op3 = modernButton("Invia operazioni");
        JButton op4 = modernButton("Chiudi client");

        op1.setFont(MODERN_BOLD);
        op2.setFont(MODERN_BOLD);
        op3.setFont(MODERN_BOLD);
        op4.setFont(MODERN_BOLD);

        centerPanel.add(op1);
        centerPanel.add(op2);
        centerPanel.add(op3);
        centerPanel.add(op4);

        menu.add(centerPanel, BorderLayout.CENTER);

        op1.addActionListener(e -> cardLayout.show(mainPanel, "calc"));
        op2.addActionListener(e -> {
            aggiornaListaOperazioni();   // aggiorna la lista prima di mostrare la card
            cardLayout.show(mainPanel, "nuova");
        });
        
        op4.addActionListener(e -> System.exit(0));

        return menu;
    }

    // ------------------------------------------------------
    //   HEADER PERSONALIZZATO
    // ------------------------------------------------------
    private static JPanel intestazione(JFrame frame) {
        JPanel header = new JPanel(null); 
        header.setBackground(new Color(28, 28, 30));
        header.setPreferredSize(new Dimension(800, 44));
        
        // Linea sottile di separazione
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE));
    
        JLabel title = new JLabel("Client Calcolatrice", SwingConstants.CENTER);
        title.setForeground(TEXT_LIGHT);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setBounds(0, 0, 800, 44);
    
        // Bottoni finestra stile macOS
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
        close.addActionListener(e -> System.exit(0));
    
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
    
        // Drag per spostare la finestra
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

        JFrame frame = new JFrame();
        frame.setSize(470, 600);
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(menuPanel(), "menu");
        mainPanel.add(calcolatricePanel(), "calc");
        mainPanel.add(nuovaPaginaPanel(), "nuova");

        frame.add(intestazione(frame), BorderLayout.NORTH);
        frame.add(mainPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}