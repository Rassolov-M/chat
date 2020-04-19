package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;

    private String nick;
    private String login;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private static Handler fileHandler;

    public ClientHandler(Socket socket, Server server) {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        try {
            this.socket = socket;
            System.out.println("RemoteSocketAddress:  " + socket.getRemoteSocketAddress());
            this.server = server;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());



            new Thread(() -> {
                try {
//                    socket.setSoTimeout(120000);

                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/reg ")) {
                            logger.log(Level.FINE, "Пользователь пытается аутентифицироваться");
                            //System.out.println("сообщение с просьбой регистрации прошло");
                            String[] token = str.split(" ");
                            boolean b = server
                                    .getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (b) {
                                logger.log(Level.FINE, "Регистрация прошла успешно");
//                                sendMsg("Регистрация прошла успешно");
                            } else {
                                logger.log(Level.FINE, "Логин или ник уже занят");
//                                sendMsg("Логин или ник уже занят");

                            }
                        }


                        if (str.equals("/end")) {
                            throw new RuntimeException("Клиент отключился крестиком");

                        }
                        if (str.startsWith("/auth ")) {
                            String[] token = str.split(" ");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);

                            login = token[1];

                            if (newNick != null) {
                                if (!server.isLoginAuthorized(login)) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    server.subscribe(this);
                                    logger.log(Level.FINER,"User " + this.nick + " прошел аутентификацию");
//                                    System.out.println("Клиент " + nick + " прошел аутентификацию");
//                                    socket.setSoTimeout(0);
                                    break;
                                } else {
                                    logger.log(Level.FINER,"User " + this.nick + " С этим логином уже авторизовались");
//                                    sendMsg("С этим логином уже авторизовались");
                                }
                            } else {
                                logger.log(Level.FINER,"User " + this.nick + " Неверный логин / пароль");
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }


                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }

                            if (str.startsWith("/w ")) {
                                String[] token = str.split(" ", 3);
                                if (token.length == 3) {
                                    server.privateMsg(this, token[1], token[2]);
                                }
                            }

                            if (str.startsWith("/chnick ")) {
                                String[] token = str.split(" ", 2);
                                if (token[1].contains(" ")) {
                                    logger.log(Level.INFO,"User " + this.nick + " Ник не может содержать пробелов");
//                                    sendMsg("Ник не может содержать пробелов");
                                    continue;
                                }
                                if (server.getAuthService().changeNick(this.nick, token[1])) {


                                    logger.log(Level.INFO,"User " + this.nick + "/yournickis" + token[1]);
                                    logger.log(Level.INFO,"User " + this.nick + "Ваш ник изменен на " + token[1]);
//                                    sendMsg("/yournickis " + token[1]);
//                                    sendMsg("Ваш ник изменен на " + token[1]);
                                    this.nick = token[1];
                                    server.broadcastClientList();
                                } else {
                                    logger.log(Level.INFO,"User " + this.nick + "Не удалось изменить ник. Ник " + token[1] + " уже существует");
//                                    sendMsg("Не удалось изменить ник. Ник " + token[1] + " уже существует");
                                }
                            }

                        } else {
                            server.broadcastMsg(nick, str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    logger.log(Level.INFO,"Клиент отключился по таймауту");
//                    System.out.println("Клиент отключился по таймауту");
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    logger.log(Level.INFO,"Клиент отключился");
//                    System.out.println("Клиент отключился");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public String getLogin() {
        return login;
    }
}
