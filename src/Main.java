import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final int PORT = 2121;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur FTP, écoute sur le port " + PORT);

            while (true) {
                // Accepter une connexion client
                Socket clientSocket = serverSocket.accept();

                // service ready
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write("220 Service ready\r\n".getBytes());
                System.out.println("Nouveau client connecté sur le port : " + clientSocket.getPort());

                // LOGIN
                InputStream inputStream = clientSocket.getInputStream();
                Scanner scanner = new Scanner(inputStream);
                String str = scanner.nextLine();

                if (Objects.equals(str, "USER miage")) {
                    System.out.println("name : " + str);
                    outputStream.write("331 username ok\r\n".getBytes());
                } else {
                    outputStream.write("530 invalid username\r\n".getBytes());
                }


                // PASSWORD


                // Fermeture des connexions
                //clientSocket.close();
                //serverSocket.close();
            }

        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        }
    }
}