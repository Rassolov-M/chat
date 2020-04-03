package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String nick;
    private String login;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            System.out.println("RemoteSocketAddress:  " + socket.getRemoteSocketAddress());
            this.server = server;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
//                    socket.setSoTimeout(3000);

                    //цикл аутентификации
                    while (true) {
//                        String str = in.readUTF();
//                        if(str.startsWith("/reg ")){
//                            System.out.println("сообщение с просьбой регистрации прошло");
//                            String[] tokens = str.split(" ");
//                            if(nick != null && !server.isNickBusy(nick)){
//                                sendMsg("/reg " + nick);
//                                this.nick = nick;
//                                server.subscribe(this);
//                                break;
//                            }

                        String str = in.readUTF();
                        if (str.startsWith("/reg ")) {
                            System.out.println("сообщение с просьбой регистрации прошло");
                            String[] token = str.split(" ");
                            boolean b = server
                                    .getAuthService()
                                    .changeNickname(token[1], token[2]);
                            if (b) {
                                sendMsg("Регистрация прошла успешно");
                            } else {
                                sendMsg("Логин или ник уже занят");
                            }
                        }


//                        if (str.equals("/end")) {
//                            throw new RuntimeException("Клиент отключился крестиком");
//
//                        }
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
                                    System.out.println("Клиент " + nick + " прошел аутентификацию");
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                }
                            } else {
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
                            if (str.startsWith("changenick ")){
                                String newNickname = str.split(" ", 2)[1];
                                if (newNickname.contains(" ")){
                                    sendMsg("Ник не может содержать пробелы");
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(this.nick, newNickname)){
                                    this.nick = newNickname;
                                    sendMsg("/changenick " + nick);
                                    sendMsg("Ник был изменен");
                                    server.broadcastClientList();
                                }else{
                                    sendMsg("Ник уже занят");
                                }
                            }

                        } else {
                            server.broadcastMsg(nick, str);
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Клиент отключился");
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
