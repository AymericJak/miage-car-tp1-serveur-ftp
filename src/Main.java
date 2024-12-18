import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final int PORT = 2121;

    private static void handleLogout(OutputStream outputStream) throws IOException {
        outputStream.write("221 Fin de la connection. Au revoir.\r\n".getBytes());
        System.out.println("Fin de connection");
    }

    private static void handleSizeCommand(OutputStream outputStream, String command) throws IOException {
        String fileName = command.substring(5); // Extract file name
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            outputStream.write(("213 " + file.length() + "\r\n").getBytes());
        } else {
            outputStream.write("550 File not found.\r\n".getBytes());
        }
    }

    private static void handleGetCommand(OutputStream outputStream, Socket clientSocket) throws IOException {
        System.out.println("Entrée dans get");
        InputStream inputStream = clientSocket.getInputStream();
        Scanner scanner = new Scanner(inputStream);

        String fileName = scanner.nextLine(); // Envoi du nom du fichier par le client

        File file = new File(fileName);
        if (file.exists()) {
            outputStream.write("150 File status okay; about to open data connection.\r\n".getBytes());

            

            // File transfer
            try (InputStream fileInputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, bytesRead);
                }
            }
            outputStream.write("226 Successful file transfer. Closing data connection\r\n".getBytes());

        } else {
            outputStream.write("550 File not found.\r\n".getBytes());
        }

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
                        System.out.println("password : " + password);
                        while (true) {
                            String command = scanner.nextLine();
                            System.out.println("Received command : " + command);
                            if (Objects.equals(command, "QUIT")) {
                                handleLogout(outputStream);
                                break;
                            } else if (Objects.equals(command, "SIZE")) {
                                handleSizeCommand(outputStream, command);
                            } else if (Objects.equals(command, "SYST")) {
                                outputStream.write("218 Syst mess.\r\n".getBytes());
                            } else if (Objects.equals(command, "FEAT")) {
                                outputStream.write("211 Feat mess.\r\n".getBytes());
                            } else if (Objects.equals(command, "EPSV")) {
                                outputStream.write("Entering Passive Mode (h1,h2,h3,h4,p1,p2).\r\n".getBytes());
                            } else if (Objects.equals(command, "LPSV")) {
                                outputStream.write("228 LPSV (long passive) mess.\r\n".getBytes());
                            } else if (Objects.equals(command, "RETR")) {
                                handleGetCommand(outputStream, clientSocket);
                            } else {
                                outputStream.write("502 Command non implémentée.\r\n".getBytes());
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