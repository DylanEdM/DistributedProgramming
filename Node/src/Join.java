import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Join {
    public JSONObject join(DatagramSocket socket, InetAddress address, int port) throws Exception {
        try {
            int numberOfProcessors = Runtime.getRuntime().availableProcessors();
            if (numberOfProcessors == 1) {
                System.out.println("You have insufficient hardware");
                System.exit(1);
            }
            JSONObject requestJSON = new JSONObject();
            requestJSON.put("Type", "Join");
            requestJSON.put("Maximum load", Runtime.getRuntime().availableProcessors());
            DatagramPacket request = new DatagramPacket(requestJSON.toJSONString().getBytes(), requestJSON.toJSONString().getBytes().length, address, port);
            socket.send(request);
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(10000);
            socket.receive(response);
            return (JSONObject) new JSONParser().parse(new String(response.getData()).trim());
        } catch (SocketTimeoutException e) {
            throw new Exception("Socket timed out, check details");
        } catch (RuntimeException | ParseException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
