

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Sebuah Server untuk game Gomoku
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
 *
 */
public class GomokuServer {

    /**
     * Menjalankan Aplikasi. Memasukkan cliet yang tersambung ke dalam room.
     */

    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Gomoku Server is Running");
        int nPlayer = (args.length == 0) ? 2 : Integer.parseInt(args[0]);

        try {
            while (true) {
                Vector <Game.Player> players = new Vector <Game.Player>();
                Game game = new Game();
                //players = game.new Player[nPlayer];
                Game.Player player;
                for(int i=0; i<nPlayer; i++){
                  char mark = Character.forDigit(i+1, 10);
                  player = game.new Player(listener.accept(), mark ,nPlayer);
                  players.add(player);
                }
                for(int i=0; i<nPlayer-1; i++){
                  player=players.get(i);
                  Game.Player opponent = players.get(i+1);
                  player.setOpponent(opponent);
                  players.set(i,player);
                }
                player=players.get(nPlayer-1);
                player.setOpponent(players.get(0));
                player=players.get(0);
                game.currentPlayer = player;
                for(int i=0; i<nPlayer; i++){
                  player=players.get(i);
                  player.start();
                }
            }
        } finally {
            listener.close();
        }
    }
}


/**
 * Sebuah game multiplayer
 */

class Game {

    /**
     * papan berukuran 20 x 20 has nine squares.  setiap kotak bisa tidak dimiliki
     * atau sudah dimiliki oleh Player. Dalam hal ini digunakan array of player
     * Jika null, kotak yang berkaitan berarti tidak sedang dimiliki player manapun,
     * Jika kotak pada array mengandung reference ke player, berarti player tersebut
     * sudah memiliki kotak tersebut.
     */
    private Player[] board;

    public Game(){
      board = new Player[400];
    }

    /**
     * The current player.
     */
    Player currentPlayer;

    /**
     * Mengembalikan true jika kondisi saat ini menunjukkan bahwa ada pemain
     * yang sudah memenangkan permainan
     */
    public boolean hasWinner() {
      return horizontalWin() || verticalWin() || rDiagonalWin() || lDiagonalWin();
    }

    /**
     * Mengembalikan true jika semua kotak sudah penuh
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * dipanggil oleh threads player ketika player mencoba untuk melakukan
     * perpindahan (mengisi kotak). Method ini mengecek apakah perpindahan
     * yang dilakukan adalah legal: yaitu pemain melakukan perpindahan pada
     * gilirannya (player adalah current player), dan kotak yang ingin dia
     * tandai tidak sedang dimiliki oleh siapapun.
     * Jika perpindahannya legal, maka state game akan diperbarui.
     * (kotak akan ditandai dan nextpalyer menjadi currentPlayer) dan
     * pemain yang lain diberitahu tentang perpindahan yang terjadi
     * sehingga dia bisa mengupdate clientnya.
     */

    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            char mark=currentPlayer.mark;
            Player tempPlayer=currentPlayer.opponent;
            for (int i=1; i<currentPlayer.nPlayer;i++){
              currentPlayer = currentPlayer.opponent;
              currentPlayer.otherPlayerMoved(location, mark);
            }
            currentPlayer=tempPlayer;
            return true;
        }
        return false;
    }

    /**
    * dipanggi oleh method hasWinner. Method horizontalWin akan mengembalikan
    * true jika ada lima kotak dalam baris yang sama diisi oleh pemain yang sama secara.
    * berurutan
    */

    public boolean horizontalWin(){
      boolean win=false;
      int nWin=0;
      int j=0;
      int i=0;

      while(i<396 && nWin<5){
        int ni=i%20;

        if((board[i]!=null) && (ni<16)){
          win = true;
          nWin = 1;
          j=i+1;
          while(win && nWin<5){
            int nj=j%20;
            if((board[j]!=null) && (board[j]==board[j-1])){
              j++;
              nWin++;
            } else{
              nWin=0;
              win=false;
              i=j;
            }
          }
        } else {
          i++;
        }
      }
      if(nWin>=5) return true;
      else return false;
    }

    /**
    * dipanggi oleh method hasWinner. Method verticalWin akan mengembalikan
    * true jika ada lima kotak dalam kolom yang sama diisi oleh pemain yang sama secara.
    * berurutan
    */
    public boolean verticalWin(){
      return false;
    }

    /**
    * dipanggi oleh method hasWinner. Method rDiagonalWin akan mengembalikan
    * true jika ada lima kotak dalam garis diagonal (ke kanan bawah) yang sama
    * diisi oleh pemain yang sama secara berurutan
    */

    public boolean rDiagonalWin(){
      return false;
    }

    /**
    * dipanggi oleh method hasWinner. Method lDiagonalWin akan mengembalikan
    * true jika ada lima kotak dalam garis diagonal (ke kiri bawah) yang sama
    * diisi oleh pemain yang sama secara berurutan
    */
    public boolean lDiagonalWin(){
      return false;
    }

    /**
     * Kelas yang merupakan threads pembantu di aplikasi multithread server ini.
     * layer dikenali dengan mark berupa bilangan cacah dalam bentuk char
     * Untuk berkumunikasi dengan client, player memiliki socket dengan input dan
     * output streams. Karena hanya teks yang sedang dikomunikasikan kita menggunakan
     * reader and a writer.
     */

    class Player extends Thread {
        char mark;
        String nickname;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        int nPlayer;

        /**
         * Membangun thread yang menghandle socket,  mark, dan jumlah pemain yang
         * diberikan. Menginisialisasi stream fields, mengirimkan dua pesan
         * selamat datang pertama.
         */
        public Player(Socket socket, char mark, int nPlayer) {
            this.socket = socket;
            this.mark = mark;
            this.nPlayer=nPlayer;

            try {
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark + nPlayer);
                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        /**
         * Menentukan pemain lawan yang bermain setelahnya
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Mengandle pesan otherPlayerMoved.
         */
        public void otherPlayerMoved(int location, char mark) {
            output.println("OPPONENT_MOVED " + mark + location);
            output.println(
                hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        /**
         * Method run dari thread ini.
         */
        public void run() {
            try {
                // Thread hanya akan mulai setelah minimal 3 Client tersambung.
                output.println("MESSAGE All players connected");

                // Memberitahu pemain yang pertama kali tersambung bahwa ia
                //mendapatkan gilirannya.
                if (mark == '1') {
                    output.println("MESSAGE Your move");
                }

                // Secara berulang menerima pesan dari client dan memprosesnya.
                while (true) {
                    String command = input.readLine();
                    System.out.println(command);
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(6));
                        if (legalMove(location, this)) {
                            output.println("VALID_MOVE");
                            //System.out.println(currentPlayer.mark);
                            output.println(hasWinner() ? "VICTORY"
                                         : boardFilledUp() ? "TIE"
                                         : "");
                        } else {
                            output.println("MESSAGE ?");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}
