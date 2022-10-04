package com.muc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
    private final int serverPort;
    private OutputStream outputStream;
    private ArrayList<ServerWorker> workerList = new ArrayList<>();
    private int agreedVersion;
    private List<Message>messageList = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<ServerWorker> getWorkerList() {
        return workerList;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while(true) {
                System.out.println("About to accept client connection...");
                Socket clientSocket = serverSocket.accept();
                this.outputStream = clientSocket.getOutputStream();
                String loginMsg = "Please use this format 'PROTOCOL? version ID: ";
                outputStream.write(loginMsg.getBytes());
                System.out.println("Accepted connection from " + clientSocket);
                ServerWorker worker = new ServerWorker(this, clientSocket);
                workerList.add(worker);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addMessage(Message message){
        messageList.add(message);
    }
    public List<Message> getMessageList(){
        return messageList;
    }

    public void removeWorker(ServerWorker serverWorker) {
        workerList.remove(serverWorker);
    }
    public void setAgreedVersion(){

        int lowcommon = workerList.get(0).getVersion();
        for(ServerWorker worker: workerList){
            if(worker.getVersion()<lowcommon){
                lowcommon = worker.getVersion();
            }
        }
        agreedVersion = lowcommon;
        System.out.println("Agreed version: " + agreedVersion);

    }
}
