import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QueueServer implements Runnable {
	Socket csocket;
	static int port = 5100;
	static int totalConnection = 0;

	static HashMap<String, ArrayList<String>> storage = new HashMap<String, ArrayList<String>>();

	QueueServer(Socket csocket) {
		this.csocket = csocket;
	}

	public static void main(String args[]) 
			throws Exception {

		System.out.println("STARTING QueueServer");

		ServerSocket ssock = new ServerSocket(port);

		Socket sock = null;

		try
		{
			System.out.println("LISTENING");
			QueueServer object = new QueueServer(sock);
			while (true) {
				sock = ssock.accept();

				new Thread(new QueueServer(sock)).start();
				object.addConnection();
				System.out.println("CONNECTION ACCEPTED");
			}
		}
		catch(Exception e)
		{

		}
		finally
		{
			if(ssock != null)
			{
				ssock.close();
			}
		}
	}
	public void run() {
		try {
			OutputStream outToServer = csocket.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);

			DataInputStream in =
					new DataInputStream(csocket.getInputStream());
			String userInput = in.readUTF();

			/*	parse the input	*/

			JsonObject output = null;
			try {
				JsonParser parser = new JsonParser();
				output = parser.parse(userInput).getAsJsonObject();
			} catch (Exception e) {

			}

			String username = output.get("username").getAsString();
			String password = output.get("password").getAsString();
			String action = output.get("action").getAsString();

			String queue = null;							
			if(output.has("queue"))
			{
				queue = output.get("queue").getAsString();
			}
			String message = null;
			if(output.has("message"))
			{
				message = output.get("message").getAsString();
			}

			/*	authenticate the input	*/
			if(!this.authenticate(username, password).equals(true))
			{
				out.writeUTF(userInput);
				return;
			}

			/*	map the request	*/

			HashMap<String, Object> result = new HashMap<String, Object>();

			switch(action)
			{
			case "put":

				put(queue, message);
				result.put("status", "OK");

				break;

			case "fetch":

				result.put("message", fetch(queue));
				result.put("status", "OK");
				break;

			case "size":
				result.put("size", fetch(queue));
				break;
			case "flush":
				result.put("status", "OK");
				break;

			case "test":
				result.put("status", "OK");
				break;
			case "create":
				createQueue(queue);
				result.put("status", "OK");
				break;
			case "destroy":
				destroyQueue(queue);
				result.put("status", "OK");
				break;
			case "setAuthentication":
				result.put("status", "OK");
				break;
			case "listQueue":
				result.put("message", listQueue());
				result.put("status", "OK");
				break;

			}

			/*	send the response	*/

			Gson gson = new Gson();
			out.writeUTF(gson.toJson(result));
			System.out.println(gson.toJson(result));

		}
		catch (IOException e) {
			System.out.println(e);
		}
		finally
		{

			try {
				csocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			reduceConnection();
		}
	}

	public synchronized void addConnection()
	{
		totalConnection++;
	}

	public synchronized void reduceConnection()
	{
		totalConnection--;
	}


	public Boolean authenticate(String username, String password)
	{
		/*	logic to authentication	*/

		return true;
	}

	public synchronized String fetch(String queue)
	{
		String result = null;
		try
		{
			if(!isQueueExist(queue).equals(true))
			{
				createQueue(queue);
			}

			int index = storage.get(queue).size() -1;
			result = (String) storage.get(queue).get(index);	
			storage.get(queue).remove(index);
			return result;
		}
		catch(Exception e)
		{
			return result;
		}

	}

	public synchronized void put(String queue, String message)
	{
		if(!isQueueExist(queue).equals(true))
		{
			createQueue(queue);
		}

		storage.get(queue).add(message);		
		return;
	}


	public Boolean isQueueExist(String queue)
	{
		Boolean status = false;
		if(storage.containsKey(queue))
		{
			status = true;
		}
		else
		{
			status = false;
		}
		return status;
	}

	public void createQueue(String queue)
	{
		ArrayList<String> container = new ArrayList<String>();
		storage.put(queue, container);
	}

	public void destroyQueue(String queue)
	{
		if(storage.containsKey(queue))
		{
			storage.remove(queue);
		}
		else
		{

		}
	}

	public ArrayList<String> listQueue()
	{
		ArrayList<String> result = new ArrayList<String>();
		for (String key : storage.keySet()) {
			result.add(key);
		}

		return result;
	}

}