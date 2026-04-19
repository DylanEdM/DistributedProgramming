import org.apache.commons.cli.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.concurrent.ConcurrentHashMap;
class Task extends Thread {
    String taskID;
    String task;
    String status;
    String clientID;
    Task(String taskID, String task,String status,String clientID) {
        this.taskID = taskID;
        this.task = task;
        this.status = status;
        this.clientID = clientID;
    }
    public void run() {
        try {
            sleep(Long.parseLong(task));
            while (!this.status.equals("Completed Successfully")) {
                this.status = "Completed Successfully";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
class PollTasks extends Thread {
    DatagramSocket socket;
    String workerID;
    ConcurrentHashMap<String,Task> tasks;
    InetAddress addr;
    int port;
    PollTasks(DatagramSocket socket,String workerID,ConcurrentHashMap<String,Task> tasks,InetAddress addr,int port) {
        this.socket = socket;
        this.workerID = workerID;
        this.tasks = tasks;
        this.addr = addr;
        this.port = port;
    }
    public void run() {
        while (true) {
            try {
                for (Task task : tasks.values()) {
                    if (!task.status.equals("Running")) {
                        JSONObject responseJSON = new JSONObject();
                        responseJSON.put("Type", "Task Status");
                        responseJSON.put("Task",task.task);
                        responseJSON.put("TaskID", task.taskID);
                        responseJSON.put("Status", task.status);
                        responseJSON.put("WorkerID", workerID);
                        responseJSON.put("ClientID",task.clientID);
                        DatagramPacket packet = new DatagramPacket(responseJSON.toJSONString().getBytes(), responseJSON.toJSONString().getBytes().length, addr, port);
                        socket.send(packet);
                        tasks.remove(task.taskID);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
void main(String[] args) {
    Options options = new Options();
    Option address = new Option("a", "address", true, "Address of load balancer");
    address.setRequired(true);
    options.addOption(address);
    Option port = new Option("p", "port", true, "Port of load balancer");
    port.setRequired(true);
    options.addOption(port);
    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;
    InetAddress addr = null;
    String workerID = null;
    try {
        cmd = parser.parse(options, args);
    } catch (ParseException e) {
        IO.println(e.getMessage());
        formatter.printHelp("LoadBalancer", options);
        System.exit(1);
    }
    try {
        addr = InetAddress.getByName(cmd.getOptionValue("address"));
    } catch (UnknownHostException e) {
        IO.println("Invalid address");
        System.exit(1);
    }
    int portNum = Integer.parseInt(cmd.getOptionValue("port"));
    DatagramSocket socket = null;
    try {
        socket = new DatagramSocket(0);
    } catch (SocketException e) {
        e.printStackTrace();
        System.exit(1);
    }
    try {
        Join joiner = new Join();
        JSONObject responseJSON = joiner.join(socket, addr, portNum);
        if (responseJSON.get("Type").equals("Join accepted")) {
            IO.println("Join accepted");
            workerID = responseJSON.get("WorkerID").toString();
        } else {
            IO.println("Request denied");
            System.exit(1);
        }
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
    ConcurrentHashMap<String,Task> tasks = new ConcurrentHashMap<>();
    new PollTasks(socket,workerID,tasks,addr,portNum).start();
    while (true) {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            socket.setSoTimeout(0);
            socket.receive(packet);
            JSONObject request =  (JSONObject) new JSONParser().parse(new String(packet.getData()).trim());
            if (request.get("Type").equals("Work request")) {
                Task task = new Task(request.get("TaskID").toString(), request.get("Task").toString(), "Running",request.get("ClientID").toString());
                tasks.put(task.taskID, task);
                System.out.println(task.taskID);
                task.start();
            }
        } catch  (Exception e) {
            IO.println(e.getMessage());
        }
    }
}
