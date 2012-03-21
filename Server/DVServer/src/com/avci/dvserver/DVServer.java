package com.avci.dvserver;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;

import com.avci.dvserver.connection.*;
import com.avci.dvserver.listener.*;
import com.avci.dvserver.npc.NPC;

public class DVServer {
	static String version = "2.1";
	
	static String mysqlHost = "localhost";
	static String mysqlUser = "root";
	static String mysqlPass = "";
	static String mysqlDB = "game";

	static int tcpPort = 6789;
	static int udpPort = 9876;

	public static MySQLConnection mysql;
	public static TCPServer tcp;
	public static UDPServer udp;

	public static HashMap<Integer, User> users = new HashMap<Integer, User>();
	public static HashMap<Integer, NPC> npcs = new HashMap<Integer, NPC>();
	public static HashSet<Point> walls = new HashSet<Point>();
	public static String mapString = "";
	static int npcId;
	private static Thread npcsThread;

	static Random r = new Random();
	private static Thread spawnerThread;
	private static Thread healerThread;

	public static Point gameLimit = new Point();

	public static void main(String[] args) {
		out("UDP & TCP Server and MySQL connection starting.");

		mysql = new MySQLConnection();
		mysql.connect(mysqlHost, mysqlUser, mysqlPass, mysqlDB);
		mysql.query("SET NAMES `UTF8`");
		out("MySQL Connected!");

		tcp = new TCPServer(tcpPort);
		tcp.setListener(new TCPListener());
		out("TCP Server Started (Port: " + tcpPort + ")");

		udp = new UDPServer(udpPort);
		udp.setListener(new UDPListener());
		out("UDP Server Started (Port: " + udpPort + ")");
		
		int[][][] map = readMapFile("src/maps/map1.gif");
		gameLimit.setLocation(map.length, map[0].length);
		for(int x = 0; x<gameLimit.x; x++) {
			for(int y = 0; y<gameLimit.y; y++) {
				int floor = map[x][y][0];
				int npcTypeId = map[x][y][1];
				mapString += floor + "|";
				
				if(floor == 70) addWall(x, y);
				
				if(npcTypeId != 255 && npcTypeId != 0) {
					spawnNPC(npcTypeId, x*32, y*32);
				}
			}
		}
		
		mapString.substring(0, mapString.length()-1);

		npcsThread = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					synchronized (npcs) {
						Collection<NPC> col = npcs.values();
						final Iterator<NPC> itr = col.iterator();
						while (itr.hasNext()) {
							NPC npc = itr.next();
							npc.tick();
							if (!npc.live) {
								npc.kill();
								itr.remove();
							}
						}
					}
				}
			}
		};

		npcsThread.start();

		spawnerThread = new Thread() {
			public void run() {
				while (true) {
					synchronized (npcs) {
						if (npcs.size() < 10) {
							int dice = r.nextInt(6) + 1;
							spawnNPC(dice, r.nextInt(500), r.nextInt(500));
						}
					}

					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};

		// spawnerThread.start();

		healerThread = new Thread() {
			public void run() {
				while (true) {
					synchronized (users) {
						Collection<User> col = users.values();
						Iterator<User> itr = col.iterator();
						while (itr.hasNext()) {
							User u = itr.next();
							if (u.data == null || u.x > 32 || u.y > 32)
								continue;

							int health = u.data.getInt("health");
							int maxHealth = u.data.getInt("maxHealth");
							
							if (health == maxHealth)
								continue;
							
							health += 10;
							
							if (health > maxHealth)
								health = maxHealth;

							if (u.data.getInt("health") != health) {
								u.data.put("x", u.x);
								u.data.put("y", u.y);
								u.data.put("health", health);
								u.tcp.send("me|" + u.data.toString());
								tcp.echo("user|" + u.data.toString(), u.tcp);
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		};
		healerThread.start();

		out("Dehset-ul Vahset Online!");
	}

	public static void hitUser(int userID, int power) {
		if (users.get(userID) == null || users.get(userID).data == null)
			return;
		User u = users.get(userID);
		int health = u.data.getInt("health");

		health -= power;
		if (health <= 0) {
			power += health;
			health = 0;

			u.data.put("money", "0");
			u.x = 0;
			u.y = 0;
			u.data.put("x", "0");
			u.data.put("y", "0");
			u.data.put("freezeTimer", "0");
			u.data.put("health", u.data.get("maxHealth"));

			u.tcp.send("me|" + u.data.toString());
			tcp.echo("user|" + u.data.toString(), u.tcp);
		} else {
			u.data.put("health", "" + health);
			udp.echo("get_damage|" + userID + "|" + power + "|" + health, null);
		}
	}

	public static NPC spawnNPC(int typeId, int x, int y) {
		npcId++;
		NPC npc = NPC.fromTypeID(typeId, npcId, x, y);
		npcs.put(npcId, npc);
		return npc;
	}

	public static void sendReward(User u, int money, int exp) {
		u.tcp.send("get_reward|" + money + "|" + exp);

		money = u.data.getInt("money") + money;
		exp = u.data.getInt("exp") + exp;

		u.data.put("money", money);
		u.data.put("exp", exp);
	}

	public static void addWall(int x, int y) {
		walls.add(new Point(x, y));
	}

	public static int[][][] readMapFile(String filename) {
		BufferedImage img = null;
		int w, h;
		int pixels[];
		int[][][] results = null;
		
		try {
			img = ImageIO.read(new File(filename));
			w = img.getWidth();
			h = img.getHeight();
			results = new int[w][h][3];
			pixels = new int[w * h];
			int i = 0;
			
			img.getRGB(0, 0, w, h, pixels, 0, w);
			
			for(int y = 0; y<h; y++) {
				for(int x = 0; x<w; x++) {
					results[x][y][0] = (pixels[i] >> 16) & 0xff;
					results[x][y][1] = (pixels[i] >> 8) & 0xff;
					results[x][y][2] = (pixels[i]) & 0xff;
					i++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return results;
	}

	public static void out(String message) {
		System.out.println(message);
	}
}
