import java.util.concurrent.ConcurrentHashMap;
void main() {
    ConcurrentHashMap<Client,ConcurrentLinkedQueue<Task>> ClientQueues= new ConcurrentHashMap<>();
    CopyOnWriteArrayList<Worker> workers =  new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<Task> tasksDone =  new CopyOnWriteArrayList<>();
    new ClientManger(ClientQueues,tasksDone).start();
    new WorkerManager(workers,tasksDone).start();
    new TaskManager(ClientQueues,workers).start();
}