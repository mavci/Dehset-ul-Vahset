package com.avci.dehsetulvahset;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPSocket {
	DatagramSocket socket = null;
	private int port;
	private InetAddress IP;
	private boolean running = true;
	DataRecerviedListener listener;
	Thread thread;

	public UDPSocket(String IP, int port) {
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		try {
			this.IP = InetAddress.getByName(IP);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		this.port = port;

		this.thread = new Thread(new Runnable() {
			@Override
			public void run() {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				
				while (true) {
					if (!running) break;
					
					try {
						socket.receive(receivePacket);

						String rawData = new String(receivePacket.getData());
						final String data = rawData.substring(0, receivePacket.getLength());
						new Thread() {
							public void run() {
								listener.DataRecervied(data);
							}
						}.start();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		thread.start();
	}

	public void send(String data) {
		byte[] sendData = data.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IP, port);
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setListener(DataRecerviedListener listener) {
		this.listener = listener;
	}

	public void close() {
		socket.close();
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

	public interface DataRecerviedListener {
		public void DataRecervied(String data);
	}
}
