import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class Server {

    // Palette colori
    private static final Color BG_DARK = new Color(18, 18, 18);
    private static final Color DISPLAY_BG = new Color(24, 24, 26);
    private static final Color TEXT_LIGHT = new Color(245, 245, 247);

    private static int clientCounter = 0;
    private static Buffer chatBuffer = new Buffer();
    private static final java.util.List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());

    public static JPanel serverPanel() {
        JPanel server = new JPanel(new BorderLayout());
        server.setBackground(BG_DARK);

        // Titolo
        JLabel titleLabel = new JLabel("Server", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 30, 0));
        server.add(titleLabel, BorderLayout.NORTH);

        // Console
        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setBackground(DISPLAY_BG);
        consoleArea.setForeground(TEXT_LIGHT);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(consoleArea);
        scrollPane.setBorder(null); // Rimuove il bordo bianco
        scrollPane.getViewport().setBackground(DISPLAY_BG);

        server.add(scrollPane, BorderLayout.CENTER);

        // Redirect System.out e System.err sulla JTextArea
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append(String.valueOf((char) b));
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                });
            }
            @Override
            public void write(byte[] b, int off, int len) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append(new String(b, off, len));
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                });
            }
        });

        System.setOut(printStream);
        System.setErr(printStream);

        return server;
    }

    public static void main(String[] args) {
        // GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Server GUI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(serverPanel());
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        // Logica server
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server in ascolto sulla porta 5000...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId;
                synchronized (Server.class) { clientId = ++clientCounter; }

                System.out.println("Client connesso! ID = " + clientId);

                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;
        private ObjectInputStream inObj;
        private ObjectOutputStream outObj;
        private volatile boolean running = true;

        public ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
        }

        public void inviaCodata() {
            try {
                synchronized(chatBuffer) {
                    outObj.reset();
                    outObj.writeObject(chatBuffer.get());
                    outObj.flush();
                }
            } catch (IOException e) {
                running = false;
            }
        }

        @Override
        public void run() {
            try {
                outObj = new ObjectOutputStream(clientSocket.getOutputStream());
                inObj = new ObjectInputStream(clientSocket.getInputStream());
        
                outObj.writeObject(clientId);
                outObj.flush();
        
                System.out.println("Client " + clientId + " pronto: " + clientSocket.getInetAddress());
        
                while (running) {
                    try {
                        Object obj = inObj.readObject();
        
                        if (obj instanceof Operazione) {
                            Operazione op = (Operazione) obj;
        
                            double risultato = switch (op.tipo) {
                                case "+" -> op.a + op.b;
                                case "-" -> op.a - op.b;
                                case "*" -> op.a * op.b;
                                case "/" -> op.b != 0 ? (double) op.a / op.b : 0;
                                default -> 0;
                            };
        
                            System.out.println("[ CALC ] Client " + clientId + ": " + op.toString() + " = " + risultato);
                            
                            synchronized(outObj) {
                                outObj.writeObject(op.toString() + " = " + risultato);
                                outObj.flush();
                            }
                        }
                        else if (obj instanceof Messaggio) {
                            Messaggio msg = (Messaggio) obj;
                            
                            synchronized(chatBuffer) {
                                chatBuffer.put(msg);
                            }
                            
                            System.out.println("[ CHAT ] " + msg.get());
                            
                            // Prima invia conferma al mittente
                            synchronized(outObj) {
                                outObj.writeObject("MSG_OK");
                                outObj.flush();
                            }
                            
                            // Poi broadcast a tutti gli ALTRI client
                            broadcastMessaggio(msg);
                        }
                        else if (obj instanceof String) {
                            String cmd = (String) obj;
                            if(cmd.equals("RICHIESTA_CODA")) {
                                System.out.println("[ CHAT ] Client " + clientId + " richiede la coda");
                                inviaCodata();
                            }
                        }
        
                    } catch (EOFException e) {
                        System.out.println("Client " + clientId + " disconnesso.");
                        break;
                    }
                }
        
            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnesso: " + e.getMessage());
            } finally {
                running = false;
                clientHandlers.remove(this);
                try { clientSocket.close(); } catch (Exception ignored) {}
            }
        }
        
        private void broadcastMessaggio(Messaggio msg) {
            synchronized(clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    if (handler.clientId != this.clientId) { // Non rimandare al mittente
                        try {
                            synchronized(handler.outObj) {
                                handler.outObj.reset();
                                handler.outObj.writeObject(msg);
                                handler.outObj.flush();
                            }
                            System.out.println("[ BROADCAST ] Messaggio inviato a Client " + handler.clientId);
                        } catch (IOException e) {
                            System.err.println("[ ERROR ] Errore broadcast a client " + handler.clientId + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
