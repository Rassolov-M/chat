package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.*;


public class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static Handler fileHandler;


    public AuthService getAuthService() {
        return authService;
    }

    public Server() throws IOException {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        clients = new Vector<>();
//        authService = new SimpleAuthService();
        if (!SQLHandler.connect()) {

            RuntimeException e = new RuntimeException("Невозможно подключиться к");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
//            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DBAuthServise();


        ServerSocket server = null;
        Socket socket = null;

        try {
            server = new ServerSocket(8189);
            //System.out.println("Сервер запущен");
            logger.severe("Сервер запущен");


            while (true) {
                socket = server.accept();
                logger.log(Level.INFO, "Клиент подключился");
//                System.out.println("Клиент подключился");

                new ClientHandler(socket, this);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            logger.log(Level.INFO, "Сервер отключен");
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(String nick, String msg) {
        logger.log(Level.FINEST,"User " + nick + " sent a message");
        for (ClientHandler c : clients) {
            c.sendMsg(nick + ": " + msg);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] private [ %s ] : %s",
                sender.getNick(), receiver, msg);

        if (sender.getNick().equals(receiver)) {
            sender.sendMsg(message);
            logger.log(Level.FINEST,"User " + sender.getNick() + " wrote a note");
            return;
        }

        for (ClientHandler c : clients) {
            if (c.getNick().equals(receiver)) {
                c.sendMsg(message);
                sender.sendMsg(message);
                logger.log(Level.FINEST,"User " + sender.getNick() + " sent a private message");
                return;
            }
        }

        sender.sendMsg("not found user: " + receiver);
        logger.log(Level.FINEST,
                "User " + sender.getNick() + " попытался отправить личное сообщение несуществующему клиенту");
        //
    }


    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
        logger.log(Level.FINE, "User " + clientHandler.getNick() + " connected");
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
        logger.log(Level.FINE, "User " + clientHandler.getNick() + " disconnected");
    }

    public boolean isLoginAuthorized(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");
        for (ClientHandler c : clients) {
            sb.append(c.getNick() + " ");
        }

        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

}
