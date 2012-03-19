package com.avci.dvserver.listener;

import java.util.HashMap;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;
import com.avci.dvserver.connection.UDPServer;
import com.avci.dvserver.connection.UDPServer.UDPDataRecerviedListener;
import com.avci.dvserver.npc.NPC;

public class UDPListener implements UDPDataRecerviedListener {
	private UDPServer udp;
	private HashMap<Integer, User> users;
	private HashMap<Integer, NPC> npcs;
	
	public UDPListener() {
		this.udp = DVServer.udp;
		this.users = DVServer.users;
		this.npcs = DVServer.npcs;
	}
	
	@Override
	public void UDPDataRecervied(String data, com.avci.dvserver.connection.UDPServer.Client c) {
		String[] datas = data.split("\\|");

		if (datas[0].equals("connect") && datas.length == 3) {
			int id = Integer.parseInt(datas[1]);
			String secret = datas[2];

			if (users.get(id) == null) {
				c.send("error");
			} else {
				User u = users.get(id);
				if (u.secret.equals(secret)) {
					u.udp = c;
					c.id = id;
					c.send("ok");
				} else {
					c.send("wrong");
				}
			}
		} else if (datas[0].equals("move") && datas.length == 3) {
			if (users.get(c.id) == null || users.get(c.id).udp != c)
				return;
			udp.echo("move|" + c.id + "|" + datas[1] + "|" + datas[2], c);
			users.get(c.id).x = Integer.parseInt(datas[1]);
			users.get(c.id).y = Integer.parseInt(datas[2]);
		} else if (datas[0].equals("attack") && datas.length == 3) {
			if (users.get(c.id) == null || users.get(c.id).udp != c)
				return;

			int npcID = Integer.parseInt(datas[1]);
			int power = Integer.parseInt(datas[2]);
			if (npcs.get(npcID) == null) {
				users.get(c.id).tcp.send("kill_npc|" + npcID);
				return;
			}

			NPC npc = npcs.get(npcID);
			npc.getDamage(users.get(c.id), power);
		}
	}
}