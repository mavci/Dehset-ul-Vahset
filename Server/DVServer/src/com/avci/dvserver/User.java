package com.avci.dvserver;

import org.json.JSONObject;

import com.avci.dvserver.connection.TCPServer;
import com.avci.dvserver.connection.UDPServer;

public class User {
	public boolean ready = false;
	public JSONObject data;
	public int x, y, id = 0;
	public String name = null;
	public String secret = null;
	public TCPServer.Client tcp = null;
	public UDPServer.Client udp = null;
}