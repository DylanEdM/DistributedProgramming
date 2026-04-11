import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

class Client {
    DatagramSocket socket;
    InetAddress clientAddress;
    int clientPort;
    String clientID;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    ConcurrentHashMap<Client, ConcurrentLinkedQueue<Task>> clientQueues;

    Client(DatagramSocket socket, InetAddress clientAddress, int clientPort, ConcurrentHashMap<Client, ConcurrentLinkedQueue<Task>> clientQueues) {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        this.clientID = encoder.encodeToString(randomBytes);
        this.socket = socket;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.clientQueues = clientQueues;
    }
}

public class ClientManger extends Thread {
    public ArrayList<Client> clients = new ArrayList<Client>();
    public ConcurrentHashMap<Client, ConcurrentLinkedQueue<Task>> clientQueues;
    private int clientManagerPort = 4000;
    private CopyOnWriteArrayList<Task> tasksDone;
    DatagramSocket socket;

    public ClientManger(ConcurrentHashMap<Client, ConcurrentLinkedQueue<Task>> clientQueues, CopyOnWriteArrayList<Task> tasksDone) {
        this.clientQueues = clientQueues;
        this.tasksDone = tasksDone;
    }

    class PollTaskUpdates extends Thread {
        public void run() {
            while (true) {
                if (!tasksDone.isEmpty()) {
                    for (Task task : tasksDone) {
                        tasksDone.remove(task);
                        for (Client client : clients) {
                            if (client.clientID.equals(task.clientID)) {
                                JSONObject responseJSON = new JSONObject();
                                responseJSON.put("TaskID", task.taskID);
                                responseJSON.put("Status", task.Status);
                                DatagramPacket packet = new DatagramPacket(responseJSON.toJSONString().getBytes(), responseJSON.toJSONString().getBytes().length, client.clientAddress, client.clientPort);
                                try {
                                    socket.send(packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void run() {
        while (true) {
            try {
                this.socket = new DatagramSocket(clientManagerPort);
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new PollTaskUpdates().start();
        while (true) {
            try {
                DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                socket.receive(request);
                JSONObject requestJSON = (JSONObject) new JSONParser().parse(new String(request.getData()).trim());
                JSONObject responseJSON = new JSONObject();
                if (requestJSON.get("Request").equals("Can I join?")) {
                    Client client = new Client(socket, request.getAddress(), request.getPort(), this.clientQueues);
                    clientQueues.put(client, new ConcurrentLinkedQueue<Task>());
                    clients.add(client);
                    responseJSON.put("Response", "true");
                    responseJSON.put("ClientID", client.clientID);
                    DatagramPacket response = new DatagramPacket(responseJSON.toJSONString().getBytes(), responseJSON.toJSONString().getBytes().length, client.clientAddress, client.clientPort);
                    socket.send(response);
                } else if (requestJSON.get("Request").equals("Run task")) {
                    for (Client client : clientQueues.keySet()) {
                        if (client.clientID.equals(requestJSON.get("ClientID"))) {
                            clientQueues.get(client).add(new Task(requestJSON.get("Task").toString(), client.clientID,requestJSON.get("TaskID").toString()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
