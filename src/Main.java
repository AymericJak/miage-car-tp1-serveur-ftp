import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static final int PORT = 2121;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur FTP, écoute sur le port " + PORT);

            while (true) {
                // Accepter une connexion client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouveau client connecté sur le port : " + clientSocket.getPort());

            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        }
    }
}