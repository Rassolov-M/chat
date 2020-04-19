package server;

import java.io.IOException;

public class Start {
    public static void main(String[] args){
        try {
            new Server();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
