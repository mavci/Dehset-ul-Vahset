package com.avci.dvserver.connection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;

import com.avci.dvserver.DVServer;

public class TCPServer {
	ServerSocket socket;
	TCPServer server;
	HashSet<Client> clients = new HashSet<Client>();
	private TCPDataRecerviedListener listener;

	public TCPServer(final int port) {
		this.server = this;

		new Thread() {
			public void run() {
				try {
					socket = new ServerSocket(port);

					while (true) {
						Socket connection = socket.accept();
						clients.add(new Client(connection, server));
						System.out.println("+++ " + connection.getInetAddress().toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void echo(String message, Client c) {
		Iterator<Client> iter = clients.iterator();
		while (iter.hasNext()) {
			Client client = iter.next();
			if (c != client) client.send(message);
		}
	}

	public void clientDisconnected(Client me) {
		System.out.println("--- " + me.con.getInetAddress().toString());
		listener.clientDisconnected(me);
		synchronized (DVServer.users) {
			DVServer.users.remove(me.id);
		}
		DVServer.out(DVServer.users.size() + " online.");
		try {
			me.con.close();
			me.in.close();
			me.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		clients.remove(me);
	}

	public class Client {
		TCPServer server;
		public Socket con;
		public BufferedReader in;
		public DataOutputStream out;
		String incoming;
		Client me;

		public String key = null;
		public int id = 0;
		public String name = null;
		public String secret = null;

		public Client(Socket co, TCPServer ser) {
			this.me = this;
			this.con = co;
			this.server = ser;
			try {
				in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
				out = new DataOutputStream(con.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread() {
				public void run() {
					while (true) {
						try {
							if(con.isClosed()) {
								server.clientDisconnected(me);
								return;
							}
							
							if(in != null) in.skip(2);
							incoming = in == null ? null : in.readLine();

							if (incoming == null) {
								server.clientDisconnected(me);
								return;
							}
							
							new Thread() {
								public void run() {
									server.listener.TCPDataRecervied(incoming, me);
								}
							}.start();
						} catch (IOException e) {
							e.printStackTrace();
							server.clientDisconnected(me);
							return;
						}
					}
				}
			}.start();
		}

		public void send(String message) {
			try {
				out.writeBytes(message + '\n');
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setListener(TCPDataRecerviedListener listener) {
		this.listener = listener;
	}

	public interface TCPDataRecerviedListener {
		public void TCPDataRecervied(String data, TCPServer.Client c);

		public void clientDisconnected(Client me);
	}
}