import java.net.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Join {
    public JSONObject join(DatagramSocket socket, InetAddress serverAddress, int serverPort) throws Exception {
        try {
            JSONObject requestJSON = new JSONObject();
            requestJSON.put("Request","Can I join?");
            DatagramPacket packet = new DatagramPacket(requestJSON.toJSONString().getBytes(), requestJSON.toJSONString().getBytes().length, serverAddress, serverPort);
            socket.send(packet);
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout(10000);
            socket.receive(response);
            return (JSONObject) new JSONParser().parse(new String(response.getData()).trim());
        } catch (SocketTimeoutException e) {
            throw new Exception("Socket timed out, check details");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }
}
