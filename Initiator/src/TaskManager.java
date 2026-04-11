import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

class Task extends Thread {
    private String task;
    private String taskID;
    private DatagramSocket socket;
    private InetAddress serverIP;
    private int serverPort;
    public String clientID;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    Task(String task, InetAddress serverIP, int serverPort,DatagramSocket socket,String clientID) {
        this.task = task;
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        this.taskID = encoder.encodeToString(randomBytes);
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.socket = socket;
        this.clientID = clientID;
    }

    public void run() {
        System.out.println("Starting task " + taskID);
        JSONObject taskJSON = new JSONObject();
        taskJSON.put("Request", "Run task");
        taskJSON.put("Task", task);
        taskJSON.put("ClientID", clientID);
        taskJSON.put("TaskID",taskID);
        DatagramPacket taskRequest = new DatagramPacket(taskJSON.toJSONString().getBytes(), taskJSON.toJSONString().getBytes().length, this.serverIP, this.serverPort);
        try {
            socket.send(taskRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class TaskManager{
    public InetAddress serverIP;
    public int serverPort;
    private DatagramSocket socket;
    TaskManager(DatagramSocket socket,  InetAddress serverIP, int serverPort) {
        this.socket = socket;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }
    private class PollForStatus extends Thread {
        public void run() {
            while (true) {
                try {
                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                socket.setSoTimeout(0);
                socket.receive(response);
                JSONObject responseJSON = (JSONObject) new JSONParser().parse(new String(response.getData()).trim());
                System.out.println("Task ID: "+responseJSON.get("TaskID").toString()+" | Status: "+responseJSON.get("Status"));
            } catch (Exception e) {
                e.printStackTrace();}
            }
        }
    }
    public void createTask(String task, String clientID) throws Exception {
        Task taskObj = new Task(task, serverIP, serverPort,socket,clientID);
        taskObj.start();
        new PollForStatus().start();
    }
}
