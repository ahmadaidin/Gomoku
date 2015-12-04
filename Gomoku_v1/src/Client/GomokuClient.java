import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Sebuah Client untuk game Gomoku
 * Digame ini dibuat dengan membangun protokol berbasis text
 * String yang dikirim antara lain:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 399)  WELCOME <int> (dikirimkan dalam bentuk char)
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 *
 */
public class GomokuClient {

    private JFrame frame = new JFrame("Gomoku");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon[] icons;
    private ImageIcon[] bigIcons;

    private Square[] board = new Square[400];
    private Square currentSquare;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    int mark;
    int nPlayer;
    private String oppIcon;

    /**
     * Constructs client dengan menyambungkan ke server,
     * membuat layout GUI dan GUI listeners.
     */
    public GomokuClient(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Layout GUI
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(20, 20, 2, 2));
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    currentSquare = board[j];
                    out.println("MOVE " + j);}});
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, "Center");
    }

    /**
     * Thread utama client yang bertugas menerima pesan dari server
     * pesan pertama adalah "WELCOME" yang dengannya berisi mark dari client
     * dan juga jumlah player yang bermain. Kemudian masuk ke dalam loop dimana
     * client menerima pesan berupa "VALID_MOVE", "OPPONENT_MOVED", "VICTORY",
     * "DEFEAT", "TIE", "OPPONENT_QUIT atau "MESSAGE" messages,
     * dan menghandle semua pesan tersebut.  pesan "VICTORY",
     * "DEFEAT" dan "TIE" menanyakan ke user apakah ingin bermain lagi atau tidak
     * jika jawabannya adalah tidak, loop akan selesai dan
     * client mengirimkan pesan "QUIT" ke server.  jika pesan "OPPONENT_QUIT"
     * diterima maka loop akan selesai and the server
     * akan dikirimi pesan "QUIT" lagi.
     */

    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                mark = Character.getNumericValue(response.charAt(8));
                nPlayer = Character.getNumericValue(response.charAt(9));
                icons=new ImageIcon[nPlayer+1];
                bigIcons=new ImageIcon[nPlayer+1];
                for(int i=1; i<=nPlayer; i++){
                    icons[i]=new ImageIcon(i+".png");
                    bigIcons[i]=new ImageIcon(i+"_.png");
                }
                frame.setTitle("Gomoku - Player " + mark);
                frame.setIconImage(bigIcons[mark].getImage());
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    int markOpp = Character.getNumericValue(response.charAt(11));
                    messageLabel.setText("Valid move, Player "+markOpp+ " turn");
                    currentSquare.setIcon(icons[mark]);
                    currentSquare.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(17));
                    int markOpp = Character.getNumericValue(response.charAt(15));
                    int current = Character.getNumericValue(response.charAt(16));
                    //System.out.println("Player "+markOpp+" move to " + loc);
                    board[loc].setIcon(icons[markOpp]);
                    board[loc].repaint();
                    if(current==mark){
                      messageLabel.setText("Your Turn");
                    } else {
                      messageLabel.setText("Player "+current+" turn");
                    }
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("You win");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("You lose");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("You tied");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
        }
        finally {
            socket.close();
        }
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
            "Want to play again?",
            "Gomoku is Fun Fun Fun",
            JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * kumpulan persegi di window client.  Setiap persegi
     * panel putih yang bisa ditandai dengan simbol dari player
     * client memanggil setIcon() untuk mengisinya dengan simbol/icon
     * yang sesuai
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }

    /**
     * main
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            GomokuClient client = new GomokuClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(480, 540);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}
