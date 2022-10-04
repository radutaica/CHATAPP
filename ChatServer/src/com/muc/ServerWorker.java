package com.muc;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.subst.Token;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static java.lang.Integer.parseInt;

public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    public boolean started=false;


    public int getVersion() {
        return version;
    }

    private int version;
    private OutputStream outputStream;


    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ( (line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);

            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if((cmd.compareTo("PROTOCOL?"))!=0 && cmd.compareTo("BYE!")!=0 && cmd.compareTo("msg")!=0 && cmd.compareTo("TIME?")!=0 && cmd.compareTo("LIST?")!=0 && cmd.compareTo("GET?")!=0){
                    System.out.println("Wrong format");
                    handleLogoff();
                    clientSocket.close();
                    break;
                }
                if("PROTOCOL?".equals(cmd)) {
                    handleLogin(outputStream, tokens);
                    started = true;
                }

                else if(started) {
                    if(cmd.compareTo("BYE!")!=0 && cmd.compareTo("msg")!=0 && cmd.compareTo("TIME?")!=0 && cmd.compareTo("LIST?")!=0 && cmd.compareTo("GET?")!=0){

                        handleLogoff();
                        break;
                    }
                    if ("BYE!".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
                        handleLogoff();
                        break;
                    } else if ("TIME?".equalsIgnoreCase(cmd)) {
                        handleTime(tokens);
                    } else if ("LIST?".equalsIgnoreCase(cmd)) {
                        String[] tokensListHelper = StringUtils.split(line, null, 2);
                        handleList(outputStream,tokensListHelper);
                    } else if ("GET?".equalsIgnoreCase(cmd)) {
                        handleGET(tokens);
                    }
                }else{
                    clientSocket.close();
                    break;
                }

            }
        }

        clientSocket.close();
    }

    private void handleGET(String[] tokens) throws IOException {
        if (tokens.length == 3){
            Message message = new Message();
            String sha = tokens[1];
            String hash = tokens[2];
            String getMsg = "GET? "+sha+" "+hash+"\r\n";
            if(sha.compareTo("SHA-256")==0) {


                String Msg = "Message-ID: " + message.getMessageID() + "\r\n" + "Time-sent: " + message.getUnixTime() + "\r\n" + "Topic: " + message.getTopic() + "\r\n" + "Subject: " + message.getSubject() + "\r\n" + "Contents: " + message.getContents() + "\r\n";
                List<ServerWorker> workerList = server.getWorkerList();
                for (ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(getMsg);

                    }
                    if (login.equals(worker.getLogin())) {
                        if (hash.equals(message.getMessageID())) {
                            System.out.println("found message");
                            worker.send("FOUND\r\n");
                            worker.send(Msg);
                        } else {
                            worker.send("SORRY\r\n");
                        }


                    }
                }
            }
            else{
                handleLogoff();
            }

        }
    }



    private void handleList(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 2){
            Pattern headerP = Pattern.compile("^[0-9]*[1-9]+$|^[1-9]+[0-9]*$");
            Pattern sinceP = Pattern.compile("\\d{10}");
            String[] subTokens = StringUtils.split(tokens[1], null);
            Matcher match1 = sinceP.matcher(subTokens[0]);
            Matcher match = headerP.matcher(subTokens[1]);
            boolean isInt = match.find();
            boolean isDate = match1.find();
            System.out.println("Flag");

            if(isInt && isDate){

                long Time = Instant.now().getEpochSecond();
                int sinceTime = parseInt(subTokens[0]);


                if(sinceTime < Time){


                    String nrHeaders = subTokens[1];
                    List<String>lines = new ArrayList<>();
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String line = "";
                    int x = 0;

                    while(x<parseInt(nrHeaders) && (line = reader2.readLine())!= null){

                        String[] fragments = line.split(" ");
                        if(fragments[0].equalsIgnoreCase("Message-ID:")){
                            Pattern sha256 = Pattern.compile("^[a-fA-F0-9]{64}$");
                            Matcher matcher2 = sha256.matcher(fragments[2]);
                            boolean isSha256 = matcher2.find();
                            if(isSha256){
                                lines.add(line);
                            }else{
                                System.out.println("wrong header input");
                                handleLogoff();
                                break;
                            }
                        }else if(fragments[0].equalsIgnoreCase("Topic:")){

                            lines.add(line);
                        }else if(fragments[0].equalsIgnoreCase("Subject:")){
                            lines.add(line);
                        }else if(fragments[0].equalsIgnoreCase("Time-sent:")){
                            lines.add(line);
                        }else if(fragments[0].equalsIgnoreCase("From:")){
                            lines.add(line);
                        }else if(fragments[0].equalsIgnoreCase("To:")){
                            lines.add(line);
                        }else if(fragments[0].equalsIgnoreCase("Contents:")){
                            lines.add(line);
                        }
                        x++;
                    }

                    System.out.println("closed while");


                    List<Message> foundMessages = findMessages(lines, sinceTime);
                    int messages = foundMessages.size();
                    String messageCount = "MESSAGES "+ messages+"\r\n";
                    List<ServerWorker> workerList = server.getWorkerList();
                    for (ServerWorker worker : workerList) {
                        if (!login.equals(worker.getLogin())) {
                            worker.send("LIST?");
                        }
                    }
                    for (ServerWorker worker : workerList) {
                        if (login.equals(worker.getLogin())) {
                            worker.send(messageCount);
                            System.out.println("Found messages"+messages+"\r\n");
                            for (Message message : foundMessages) {

                                worker.send(message.getMessageID()+"\r\n");

                            }
                        }
                    }


                    }


                }


            }

        }
    public List<Message> findMessages(List<String> lines, int since){

        List<Message> storedMessages = server.getMessageList();
        List<Message> foundMessages = new ArrayList<>();
        System.out.println(storedMessages.size());
        for(Message message : storedMessages){
            int count = 0;
            if (message.getUnixTime()> since){
                for(String line:lines){

                    String[] tokens = StringUtils.split(line, null);
                    if(tokens[0].equalsIgnoreCase("Message-ID:")){
                        if(message.getMessageID().equalsIgnoreCase(tokens[1])){
                            count++;
                        }


                    }else if(tokens[0].equalsIgnoreCase("Topic:")){
                        if(message.getTopic().equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }else if(tokens[0].equalsIgnoreCase("Subject:")){
                        if(message.getSubject().equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }else if(tokens[0].equalsIgnoreCase("Time-sent:")){
                        if(Long.toString(message.getUnixTime()).equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }else if(tokens[0].equalsIgnoreCase("From:")){
                        if(message.getFrom().equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }else if(tokens[0].equalsIgnoreCase("To:")){
                        if(message.getTo().equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }else if(tokens[0].equalsIgnoreCase("Contents:")){
                        if(message.getContents().equalsIgnoreCase(tokens[1])){
                            count++;
                        }
                    }
                }
                System.out.println("Flag 2: "+lines.size()+ count);
                if(count==lines.size()&&lines.size()>0){
                    System.out.println("FLAG 3!");
                    foundMessages.add(message);
                }
            }

        }
        System.out.println(lines.size());
        return foundMessages;
    }




    private void handleTime(String[] tokens) throws IOException {
        if (tokens.length == 1){
            String timeMsg = "TIME?"+"\r\n";
            long unixTime = Instant.now().getEpochSecond();
            List<ServerWorker> workerList = server.getWorkerList();
            for(ServerWorker worker : workerList) {
                if (!login.equals(worker.getLogin())) {
                    worker.send(timeMsg);
                }
                if (login.equals(worker.getLogin())) {
                    worker.send("NOW " + unixTime+"\r\n");
                }
            }

        }
    }


    /*private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        DBConnect dbConnect = new DBConnect();
        Connection connection = dbConnect.connection();
        String statement = "INSERT INTO `messageFormat` (`MessageID`, `Time-Sent`, `From`, `Topic`, `Subject`, `Content`) VALUES (?,?,?,?,?,?);";
        try(PreparedStatement preparedStatement = connection.prepareStatement(statement)){
            //preparedStatement.setString(1, messageID);
            //preparedStatement.setString(1, Time-Sent);
            //preparedStatement.setString(1, From);
            //preparedStatement.setString(1, Topic);
            //preparedStatement.setString(1, Subject);
            //preparedStatement.setString(1, String.valueOf(content);
        }catch(SQLException e){
            e.printStackTrace();
        }
        Message message = new Message();
        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "msg " + login + " " + body + "\r\n";
                worker.send(outMsg);
            }
        }
    }*/

    private void handleLogoff() throws IOException {

        server.removeWorker(this);
        String msg = "BYE!";
        System.out.println(msg);
        for (ServerWorker worker: server.getWorkerList()){
                worker.send(msg);
                clientSocket.close();
            }
        clientSocket.close();
        }





    public String getLogin() {
        return login;
    }


    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            Pattern versionpattern = Pattern.compile("^[0-9]*[1-9]+$|^[1-9]+[0-9]*$");
            Matcher matcher = versionpattern.matcher(tokens[1]);
            boolean isPosInt = matcher.find();
            if(isPosInt){
                int version = parseInt(tokens[1]);
                String login = tokens[2];
                this.login = login;
                this.version = version;
                System.out.println("User logged in succesfully: " + login);

                List<ServerWorker> workerList = server.getWorkerList();

                // send current user all other online logins
                for (ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = "PROTOCOL? " + worker.getVersion() +" "+ worker.getLogin() + "\r\n";
                            send(msg2);
                            server.setAgreedVersion();
                        }
                    }
                }


                // send other online users current user's status

                for(ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        String onlineMsg = "PROTOCOL? " + version +" "+ login + "\r\n";
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "error login\r\n";
                outputStream.write(msg.getBytes());
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write(msg.getBytes());
        }
    }
}
