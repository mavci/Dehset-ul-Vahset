package com.avci.dehsetulvahset;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPSocket {
	Socket socket = null;
	private boolean running = true;
	TCPListener listener;
	Thread thread;
	private BufferedReader inFromServer;
	private boolean connected = false;

	String IP;
	int port;

	public TCPSocket(String IP, int port) {
		this.IP = IP;
		this.port = port;
	}

	public void connect() {
		this.thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					socket = new Socket(IP, port);
					connected = true;
					listener.connected();
					inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), 1024);
					
					while (true) {
						if (!running)
							break;
						String rawRecervied = inFromServer.readLine();
						if (rawRecervied == null) {
							close();
							break;
						}
						
						final String recervied = rawRecervied;
						new Thread() {
							public void run() {
								listener.dataRecervied(recervied);
							}
						}.start();
					}
				} catch (IOException e) {
					e.printStackTrace();
					listener.disconnected();
				}
			}
		});
		thread.start();
	}

	public void send(String data) {
		if (!connected)
			return;

		DataOutputStream outToServer;
		try {
			outToServer = new DataOutputStream(socket.getOutputStream());
			outToServer.writeUTF(data + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setListener(TCPListener listener) {
		this.listener = listener;
	}

	public void close() {
		if (!connected)
			return;
		connected = false;

		try {
			inFromServer.close();
			socket.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		boolean retry = true;
		running = false;

		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public interface TCPListener {
		public void dataRecervied(String data);

		public void connected();

		public void disconnected();
	}
}
