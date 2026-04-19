import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

class Task  {
    String task;
    String taskID;
    String clientID;
    String Status = "Not Started";
    Task(String task,String clientID,String taskID) {
        this.task = task;
        this.taskID = taskID;
        this.clientID = clientID;
    }
    Task(String task, String clientID, String taskID, String status) {
        this.task = task;
        this.taskID = taskID;
        this.clientID = clientID;
        this.Status = status;
    }
}
public class TaskManager extends Thread {
    public ConcurrentHashMap<Client,ConcurrentLinkedQueue<Task>> ClientQueues;
    public ConcurrentLinkedQueue<Task> dispatchQueue = new ConcurrentLinkedQueue<>();
    public CopyOnWriteArrayList<Worker> workers;
    public TaskManager(ConcurrentHashMap<Client,ConcurrentLinkedQueue<Task>> ClientQueues,  CopyOnWriteArrayList<Worker> workers) {
        this.ClientQueues = ClientQueues;
        this.workers = workers;
    }
    class clientRequests extends Thread {
        public void run() {
            while (true) {
                for (ConcurrentLinkedQueue<Task> queue : ClientQueues.values()) {
                    if (!queue.isEmpty()) {
                        Task task = queue.poll();
                        dispatchQueue.offer(task);
                    }
                }
            }
        }
    }
    class AssignWork extends Thread {
        public void run() {
            while (true) {
                if (dispatchQueue.peek() != null && !workers.isEmpty()) {
                    Worker min = null;
                    for (Worker worker : workers) {
                        if (min == null || min.currentLoad > worker.currentLoad) {
                            min = worker;
                        }
                    }
                    min.setTask(dispatchQueue.poll());
                } else {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    public void run() {
        new clientRequests().start();
        new AssignWork().start();
    }
}
