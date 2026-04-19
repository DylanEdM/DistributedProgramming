import org.apache.commons.cli.*;
import org.json.simple.JSONObject;

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
    String clientID = null;
    try {
        cmd = parser.parse(options, args);
    } catch (ParseException e) {
        IO.println(e.getMessage());
        formatter.printHelp("LoadBalancer", options);
        System.exit(1);
    }
    try {
        addr = InetAddress.getByName(cmd.getOptionValue("address"));
    } catch (Exception e) {
        IO.println("Invalid IP address");
        System.exit(1);
    }
    int portNum = Integer.parseInt(cmd.getOptionValue("port"));
    DatagramSocket socket = null;
    try {
        socket = new DatagramSocket(0);
    } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
    }
    try {
        Join joiner = new Join();
        JSONObject responseJSON = joiner.join(socket, addr, portNum);
        if (responseJSON.get("Response").equals("true")) {
            IO.println("Successfully connected to load balancer");
            clientID = responseJSON.get("ClientID").toString();
        } else {
            IO.println("Request denied by load balancer");
            System.exit(1);
        }
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
    TaskManager taskManager = new TaskManager(socket, addr, portNum);
    Scanner sc = new Scanner(System.in);
    IO.println("Remember you can exit the program using Ctrl + z");
    while (true) {
        IO.println("Please enter, in milliseconds, the task:");
        String input = sc.nextLine();
        try {
            taskManager.createTask(input, clientID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
