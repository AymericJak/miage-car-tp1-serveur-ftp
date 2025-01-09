import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final int PORT = 2121;
    private static ServerSocket dataSocket = null;
    private static final Map<String, String> USER_CREDENTIALS = new HashMap<>();

    static {
        USER_CREDENTIALS.put("miage", "miage");
        USER_CREDENTIALS.put("root", "root");
        USER_CREDENTIALS.put("aymeric", "miage");
    }

    private static boolean handleLogin(Scanner scanner, OutputStream outputStream) throws IOException {
        String userCommand = scanner.nextLine();
        System.out.println("Commande reçue dans handleLogin : " + userCommand);

        if (userCommand.startsWith("USER ")) {
            String username = userCommand.substring(5).trim();
            if (USER_CREDENTIALS.containsKey(username)) {
                System.out.println("name : " + userCommand);
                outputStream.write("331 username ok\r\n".getBytes());
                String password = scanner.nextLine();
                password = password.substring(5).trim();
                if (Objects.equals(USER_CREDENTIALS.get(username), password)) {
                    outputStream.write("230 password ok\r\n".getBytes());
                    System.out.println("password : " + password);
                    return true;
                } else {
                    outputStream.write("430 invalid password\r\n".getBytes());
                }

            } else {
                outputStream.write("430 invalid username\r\n".getBytes());
            }
        }
        return false;
    }

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

    private static void handleRETRCommand(OutputStream outputStream, Socket clientSocket, String fileName) throws IOException {
        System.out.println("Entrée dans get pour le fichier : " + fileName);

        File file = new File("data/" + fileName);
        if (file.exists()) {

            System.out.println(dataSocket);
            if (dataSocket == null || dataSocket.isClosed()) {
                outputStream.write("425 Unable to establish a data connection.\r\n".getBytes());
                return;
            }

            outputStream.write("150 File status okay; about to open data connection.\r\n".getBytes());

            try (Socket dataConnection = dataSocket.accept(); InputStream fileInputStream = new FileInputStream(file); OutputStream dataOutputStream = dataConnection.getOutputStream()) {
                byte[] bytes = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(bytes)) != -1) {
                    dataOutputStream.write(bytes, 0, bytesRead);
                }
            } catch (IOException e) {
                outputStream.write("426 Connection closed; transfer aborted.\r\n".getBytes());
                return;
            } finally {
                dataSocket.close();
                dataSocket = null;
            }

            outputStream.write("226 Successful file transfer. Closing data connection\r\n".getBytes());

        } else {
            outputStream.write("550 File not found.\r\n".getBytes());
        }
    }

    private static void handleEPSVCommand(OutputStream outputStream) throws IOException {
        if (dataSocket != null && !dataSocket.isClosed()) {
            dataSocket.close();
        }
        dataSocket = new ServerSocket(0); // temporary port
        int port = dataSocket.getLocalPort();

        String response = "229 Entering Extended Passive Mode (|||" + port + "|)\r\n";
        outputStream.write(response.getBytes());
    }

    private static void handleLISTCommand(OutputStream outputStream, Socket clientSocket, String pathName) throws IOException {
        File directory = (pathName == null || pathName.isEmpty()) ? new File(".") : new File(pathName);

        if (!directory.exists() || !directory.isDirectory()) {
            outputStream.write("550 Directory not found.\r\n".getBytes());
            return;
        }

        outputStream.write("150 File status okay; about to open data connection.\r\n".getBytes());
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            outputStream.write("226 Directory is empty.\r\n".getBytes());
            return;
        }

        try (Socket dataConnection = dataSocket.accept(); OutputStream dataOutputStream = dataConnection.getOutputStream()) {
            StringBuilder listStr = new StringBuilder();
            for (File file: files) {
                String type = file.isDirectory() ? "d" : "-";
                String size = file.isFile() ? String.valueOf(file.length()) : "0";
                String name = file.getName();
                listStr.append(String.format("%s %10s %s\r\n", type, size, name));
            }
            dataOutputStream.write(listStr.toString().getBytes());
        } catch (IOException e) {
            outputStream.write("426 Connection closed; transfer aborted.\r\n".getBytes());
            return;
        } finally {
            dataSocket.close();
            dataSocket = null;
        }

        outputStream.write("226 Directory LIST sent successfully.\r\n".getBytes());
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
                boolean loggedIn = false;
                while (!loggedIn) {
                    loggedIn = handleLogin(scanner, outputStream);
                    if (!loggedIn) {
                        outputStream.write("Veuillez réessayer de vous connecter.\r\n".getBytes());
                    }
                }


                if (loggedIn) {
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
                            handleEPSVCommand(outputStream);
                        } else if (Objects.equals(command, "LPSV")) {
                            outputStream.write("228 Entering LPSV (long passive) mode.\r\n".getBytes());
                        } else if (command.startsWith("RETR ")) {
                            String fileName = command.substring(5).trim();
                            handleRETRCommand(outputStream, clientSocket, fileName);
                        } else if (command.startsWith("LIST ")) {
                            String pathName = command.substring(5).trim();
                            handleLISTCommand(outputStream, clientSocket, pathName);
                        } else {
                            outputStream.write("502 Command non implémentée.\r\n".getBytes());
                        }
                    }
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