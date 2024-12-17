import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final int PORT = 2121;

    private static void handleLogout(OutputStream outputStream) throws IOException {
        outputStream.write("221 Fin de la connection. Au revoir.\r\n".getBytes());
    }

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
                String userCommand = scanner.nextLine();

                if (Objects.equals(userCommand, "USER miage")) {
                    System.out.println("name : " + userCommand);
                    outputStream.write("331 username ok\r\n".getBytes());

                    String password = scanner.nextLine();
                    if (Objects.equals(password, "PASS miage")) {
                        outputStream.write("230 password ok\r\n".getBytes());
                        while (true) {
                            String command = scanner.nextLine();
                            if (Objects.equals(command, "QUIT")) {
                                handleLogout(outputStream);
                                break;
                            } else {
                                outputStream.write("502 Commande non implémentée.\r\n".getBytes());
                            }
                        }
                    } else {
                        outputStream.write("430 invalid password\r\n".getBytes());
                    }
                } else {
                    outputStream.write("430 invalid username\r\n".getBytes());
                }


                // Fermeture des connexions
                clientSocket.close();
//                serverSocket.close();
            }

        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        }
    }
}