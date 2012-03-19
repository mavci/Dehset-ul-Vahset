package com.avci.dvserver.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.avci.dvserver.DVServer;

public class UDPServer {
	public static DatagramSocket socket;
	static byte[] receiveData = new byte[1024];
	static byte[] sendData;
	static HashMap<String, Client> clients = new HashMap<String, Client>();
	private UDPDataRecerviedListener listener;

	public UDPServer(final int port) {
		new Thread() {
			public void run() {
				try {
					socket = new DatagramSocket(port);
					while (true) {
						DatagramPacket receivePacket = new DatagramPacket(receiveData, 1024);
						socket.receive(receivePacket);
						readPacket(receivePacket);
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void readPacket(final DatagramPacket receivePacket)
			throws IOException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String data = new String(receivePacket.getData()).substring(0, receivePacket.getLength());
				InetAddress IP = receivePacket.getAddress();
				int port = receivePacket.getPort();
				String key = IP.toString() + ":" + port;

				final Client client;

				if (clients.containsKey(key)) {
					client = clients.get(key);
				} else {
					client = addClient(IP, port);
				}

				client.timeout = System.currentTimeMillis() / 1000L;

				new Thread() {
					public void run() {
						listener.UDPDataRecervied(data, client);
					}
				}.start();
			}
		}).start();
	}

	public void echo(String data, Client client) {
		synchronized (clients) {
			for (Iterator<Entry<String, Client>> itr = clients.entrySet().iterator(); itr.hasNext();) {
				Entry<String, Client> entry = itr.next();
				Client c = entry.getValue();
				if (c.equals(client) || c.id == 0 || DVServer.users.get(c.id) == null || !DVServer.users.get(c.id).ready)
					continue;

				if (c.timeout < (System.currentTimeMillis() / 1000L) - 60) {
					itr.remove();
					continue;
				}

				c.send(data);
			}
		}
	}



	public Client addClient(InetAddress IP, int port) {
		System.out.println("+++ " + IP.toString() + ":" + port);
		Client client = new Client(IP, port);
		clients.put(IP.toString() + ":" + port, client);
		return client;
	}
	
	public class Client {
		static final int IDLE = 0;
		static final int LOGGED = 1;
		
		InetAddress IP;
		public int port, id = 0;
		long timeout;
		public int state = IDLE;
		public String name, phoneKey, key;
		
		public Client(InetAddress IP, int port) {
			this.IP = IP;
			this.port = port;
			this.timeout = System.currentTimeMillis() / 1000L;
			this.key = IP.toString() + ":" + port;
		}
		
		public void send(String data) {
			sendData = data.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IP, port);
			try {
				socket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setListener(UDPDataRecerviedListener listener) {
		this.listener = listener;
	}
	
	public interface UDPDataRecerviedListener {
		public void UDPDataRecervied(String data, UDPServer.Client c);
	}
}
