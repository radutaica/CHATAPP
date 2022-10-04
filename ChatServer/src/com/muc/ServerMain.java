package com.muc;


public class ServerMain {
    public static void main(String[] args) {
        int port = 20111;
        Server server = new Server(port);
        server.start();
        Message message = new Message();
        server.addMessage(message);
    }
}
