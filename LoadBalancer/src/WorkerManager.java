import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;

class Worker {
    InetAddress workerAddress;
    int workerPort;
    String workerID;
    double maxTasks = 1;
    double currentTasks = 0;
    double currentLoad = currentTasks/maxTasks;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private DatagramSocket socket;
    private CopyOnWriteArrayList<Worker> workers;
    Worker(InetAddress workerAddress, int workerPort, int maxTasks,DatagramSocket socket,CopyOnWriteArrayList<Worker> workers) {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        this.workerID = encoder.encodeToString(randomBytes);
        this.workerPort = workerPort;
        this.workerAddress = workerAddress;
        this.maxTasks = maxTasks;
        this.socket = socket;
        this.workers = workers;
    }
    public void setTask(Task task) {
        try {
            this.currentTasks += 1;
            this.currentLoad = this.currentTasks/this.maxTasks;
            JSONObject requestJSON = new JSONObject();
            requestJSON.put("Type", "Work request");
            requestJSON.put("Task", task.task);
            requestJSON.put("TaskID", task.taskID);
            requestJSON.put("ClientID",task.clientID);
            DatagramPacket request = new DatagramPacket(requestJSON.toJSONString().getBytes(), requestJSON.toJSONString().getBytes().length, this.workerAddress, workerPort);
            socket.send(request);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
public class WorkerManager extends Thread {
    CopyOnWriteArrayList<Worker> workers;
    DatagramSocket serverSocket;
    int workerPort = 5000;
    CopyOnWriteArrayList<Task> tasksDone;
    WorkerManager(CopyOnWriteArrayList<Worker> workers,CopyOnWriteArrayList<Task> tasksDone) {
        this.workers = workers;
        this.tasksDone = tasksDone;
        try {
            serverSocket = new DatagramSocket(workerPort);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() {
        while (true) {
            try {
                DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                serverSocket.receive(request);
                JSONObject requestJSON = (JSONObject) new JSONParser().parse(new String(request.getData()).trim());
                JSONObject responseJSON = new JSONObject();
                if (requestJSON.get("Type").equals("Join")) {
                    Worker worker = new Worker(request.getAddress(), request.getPort(),(int) (long) requestJSON.get("Maximum load"),serverSocket,workers);
                    workers.add(worker);
                    responseJSON.put("Type","Join accepted");
                    responseJSON.put("WorkerID",worker.workerID);
                    DatagramPacket response = new DatagramPacket(responseJSON.toJSONString().getBytes(), responseJSON.toJSONString().length(), worker.workerAddress, worker.workerPort);
                    serverSocket.send(response);
                } else if (requestJSON.get("Type").equals("Task Status")) {
                    tasksDone.add(new Task(requestJSON.get("Task").toString(),requestJSON.get("ClientID").toString(),requestJSON.get("TaskID").toString(),requestJSON.get("Status").toString()));
                    for (Worker worker : workers) {
                        if (worker.workerID.equals(requestJSON.get("WorkerID").toString())) {
                            worker.currentTasks = worker.currentTasks - 1;
                            worker.currentLoad = worker.currentTasks/worker.maxTasks;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
