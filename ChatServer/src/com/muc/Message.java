package com.muc;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private String MessageID = "bc18ecb5316e029af586fdec9fd533f413b16652bafe079b23e021a6d8ed69aa";
    private long unixTime = 1614686400;
    private String From = "martin.brain@city.ac.uk";
    private String Contents= "2\r\n" +
            "Hello everyone!\r\n" +
            "This is the first message sent using PM.\r\n";
    private String To;
    private String Subject = "Hello!";
    private String Topic = "#announcements";

    //add list of messages
    // create add method
    // create object message when program is launched
    //and then add it to the list

    public String getMessageID() {
        return MessageID;
    }

    public void setMessageID(String messageID) {
        MessageID = messageID;
    }

    public long getUnixTime() {
        return unixTime;
    }

    public void setUnixTime(long unixTime) {
        this.unixTime = unixTime;
    }

    public String getFrom() {
        return From;
    }

    public void setFrom(String from) {
        From = from;
    }

    public String getContents() {
        return Contents;
    }

    public void setContents(String contents) {
        Contents = contents;
    }

    public String getTo() {
        return To;
    }

    public void setTo(String to) {
        To = to;
    }

    public String getSubject() {
        return Subject;
    }

    public void setSubject(String subject) {
        Subject = subject;
    }

    public String getTopic() {
        return Topic;
    }

    public void setTopic(String topic) {
        Topic = topic;
    }

    public Message getMessage(){
        return this;
    }

}
