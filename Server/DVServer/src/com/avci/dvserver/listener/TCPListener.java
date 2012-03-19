package com.avci.dvserver.listener;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;
import com.avci.dvserver.connection.MySQLConnection;
import com.avci.dvserver.connection.TCPServer;
import com.avci.dvserver.connection.TCPServer.Client;
import com.avci.dvserver.connection.TCPServer.TCPDataRecerviedListener;
import com.avci.dvserver.npc.NPC;

public class TCPListener implements TCPDataRecerviedListener {
	private MySQLConnection mysql;
	private TCPServer tcp;
	private HashMap<Integer, User> users;
	private HashMap<Integer, NPC> npcs;
	
	public TCPListener() {
		this.mysql = DVServer.mysql;
		this.tcp = DVServer.tcp;
		this.users = DVServer.users;
		this.npcs = DVServer.npcs;
	}
	
	@Override
	public void TCPDataRecervied(String data, Client c) {
		out("<<< [" + (users.get(c.id) != null ? users.get(c.id).name : "?") + "] : " + data);
		
		if (data.equals("bye")) {
			try {
				c.con.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		String[] datas = data.split("\\|");

		if (datas[0].equals("key") && c.key == null && datas.length == 2) {
			c.key = MySQLConnection.slash(datas[1]);
			ResultSet user = mysql.getRow("SELECT ID, name FROM users WHERE phoneKey = \"" + c.key + "\"");

			if (user == null) {
				c.send("name");
			} else {
				try {
					int id = Integer.parseInt(user.getString(1));
					String name = user.getString(2);
					String secret = new BigInteger(130, new SecureRandom()).toString(32).substring(0, 16);

					User u;

					synchronized (users) {

						if (users.get(id) != null) {
							u = users.get(id);
						} else {
							u = new User();
							users.put(id, u);
						}

						u.name = name;
						u.secret = secret;
						u.id = id;
						u.tcp = c;
					}
					c.id = id;
					mysql.query("UPDATE users SET secret = '" + u.secret + "' WHERE ID = " + u.id);
					c.send("welcome|" + u.id + "|" + u.name + "|" + u.secret + "|" + (users.size() - 1));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else if (datas[0].equals("name") && datas.length == 2) {
			String name = MySQLConnection.slash(datas[1]);
			
			int name_exists = Integer.parseInt(mysql.getVar("SELECT COUNT(ID) FROM users WHERE name = \"" + name + "\""));
			if (name_exists != 0) {
				c.send("name_exists");
			} else {
				String secret = new BigInteger(130, new SecureRandom()).toString(32).substring(0, 16);

				int id = mysql.insert("INSERT INTO users (phoneKey, name, secret) VALUES('" + c.key + "', '" + name + "', '" + secret + "')");
				mysql.insert("INSERT INTO user_datas (userID) VALUES(" + id + ")");

				User u;

				synchronized (users) {

					if (users.get(id) != null) {
						u = users.get(id);
					} else {
						u = new User();
						users.put(id, u);
					}

					u.name = name;
					u.secret = secret;
					u.id = id;
					u.tcp = c;
				}
				c.id = id;
				
				c.send("welcome|" + u.id + "|" + u.name + "|" + u.secret + "|" + (users.size() - 1));
			}
		} else if (c.id != 0 && users.get(c.id) != null) {
			if (data.equals("getGameData")) {
				c.send("game_limit|" + DVServer.gameLimit.x + "|" + DVServer.gameLimit.y);
				c.send("map|" + DVServer.mapString);

				ResultSet rs = mysql.getResults("SELECT * FROM user_datas WHERE userID = " + c.id);
				JSONObject user_data = new JSONObject();
				try {
					rs.next();
					user_data.put("id", rs.getString(1));
					user_data.put("x", rs.getString(2));
					user_data.put("y", rs.getString(3));
					user_data.put("charX", rs.getString(4));
					user_data.put("charY", rs.getString(5));
					user_data.put("health", rs.getString(6));
					user_data.put("maxHealth", rs.getString(7));
					user_data.put("power", rs.getString(8));
					user_data.put("money", rs.getString(9));
					user_data.put("speed", rs.getString(10));
					user_data.put("level", rs.getString(11));
					user_data.put("exp", rs.getString(12));
					user_data.put("gm", rs.getString(13));
					user_data.put("name", users.get(c.id).name);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				users.get(c.id).data = user_data;
				users.get(c.id).x = Integer.parseInt(user_data.getString("x"));
				users.get(c.id).y = Integer.parseInt(user_data.getString("y"));

				c.send("me|" + user_data.toString());
				tcp.echo("user|" + user_data.toString(), c);

				String tmp_msg = "";

				synchronized (users) {
					Collection<User> col = users.values();
					Iterator<User> itr = col.iterator();
					while (itr.hasNext()) {
						User other_user = itr.next();
						if (other_user.id == c.id || other_user.data == null)
							continue;
						other_user.data.put("x", other_user.x);
						other_user.data.put("y", other_user.y);
						if (tmp_msg.length() + other_user.data.toString().length() > 1024) {
							c.send("user|" + tmp_msg.substring(0, tmp_msg.length() - 1));
							tmp_msg = other_user.data.toString() + "|";
						} else {
							tmp_msg += other_user.data.toString() + "|";
						}
					}

					if (tmp_msg.length() > 0)
						c.send("user|" + tmp_msg.substring(0, tmp_msg.length() - 1));
				}

				tmp_msg = "";

				synchronized (npcs) {
					Collection<NPC> colNPC = npcs.values();
					Iterator<NPC> itrNPC = colNPC.iterator();
					while (itrNPC.hasNext()) {
						NPC npc = itrNPC.next();
						if (tmp_msg.length() + npc.data.toString().length() > 1024) {
							c.send("npc|" + tmp_msg.substring(0, tmp_msg.length() - 1));
							tmp_msg = npc.data.toString() + "|";
						} else {
							tmp_msg += npc.data.toString() + "|";
						}
					}

					if (tmp_msg.length() > 0)
						c.send("npc|" + tmp_msg.substring(0, tmp_msg.length() - 1));
				}
				c.send("userListComplete");
				users.get(c.id).ready = true;
			} else if (datas[0].equals("spawn_npc") && datas.length == 4) {
				int npcTypeId = Integer.parseInt(datas[1]);
				int x = Integer.parseInt(datas[2]);
				int y = Integer.parseInt(datas[3]);

				synchronized (npcs) {
					NPC npc = DVServer.spawnNPC(npcTypeId, x, y);
					tcp.echo("npc|" + npc.data.toString(), null);
				}
			} else if (datas[0].equals("user_info") && datas.length == 2) {
				int id = Integer.parseInt(datas[1]);
				if (users.get(id) == null)
					return;
				User u = users.get(id);
				u.data.put("x", u.x);
				u.data.put("y", u.y);
				c.send("user|" + u.data.toString());
			} else if (datas[0].equals("npc_info") && datas.length == 2) {
				int id = Integer.parseInt(datas[1]);
				if (npcs.get(id) == null)
					return;
				NPC n = npcs.get(id);
				c.send("npc|" + n.data.toString());
			}

		}
	}

	@Override
	public void clientDisconnected(Client me) {
		User u = users.get(me.id);

		if (users.get(me.id) != null && users.get(me.id).data != null) {
			u.x = Math.round(u.x / 32) * 32;
			u.y = Math.round(u.y / 32) * 32;

			mysql.query("UPDATE user_datas SET x = " + u.x + ", y = " + u.y + ", cX = " + u.data.getString("charX") + ", cY = " + u.data.getString("charY") + ", health = "
					+ u.data.getString("health") + ", power = " + u.data.getString("power") + ", money = " + u.data.getString("money") + ", speed = " + u.data.getString("speed") + ", level = "
					+ u.data.getString("level") + ", exp = " + u.data.getString("exp") + " WHERE userID = " + me.id);

			tcp.echo("quit|" + me.id, me);
			out(u.name + " quit.");
		}
	}
	
	public static void out(String message) {
		System.out.println(message);
	}
}