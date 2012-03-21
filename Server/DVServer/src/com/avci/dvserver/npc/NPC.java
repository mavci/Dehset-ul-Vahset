package com.avci.dvserver.npc;

import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONObject;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;
import com.avci.dvserver.npc.monster.*;

public class NPC {
	public static final int MONEY = 7;
	public static final int TREASURE = 9;
	public static final int HOSTAGE = 10;
	
	public int x, y, maxHealth = 100, health = 100, power = 2, money = 5, exp = 2;
	public Point last = new Point();
	public boolean live = true;
	public int tickSpeed, tickTimer = 0, id = 0;
	public Random r = new Random();

	public JSONObject data = new JSONObject();
	
	public static class Money extends NPC {
		int notificationTimer = 0;
		public Money(int id, int x, int y) {
			super(id, x, y);
			tickSpeed = 150;
			maxHealth = health = 60;
			power = 1;
			money = 12;
			exp = 8;
			
			setSprite(8, 1);
		}
		
		public void tick() {
			if(timer()) return;
			int minDistance = -1;
			int distance = -1;
			int targetUserId = 0;
			
			Collection<User> col = DVServer.users.values();
			Iterator<User> itr = col.iterator();
			while (itr.hasNext()) {
				User u = itr.next();
				if(u.x < 64 && u.y < 64) continue;
				distance = (int) Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
				if(distance <= 32 && (minDistance > distance || minDistance == -1)) {
					minDistance = distance;
					targetUserId = u.id;
				}
			}
			
			if(targetUserId != 0) {
				synchronized (DVServer.users) {
					User target = DVServer.users.get(targetUserId);
					int newMoney = target.data.getInt("money") + money;
					target.data.put("money", newMoney);
					target.data.put("x", target.x);
					target.data.put("y", target.y);
					target.tcp.send("me|" + target.data.toString());
				}
				kill();
			}
			
			if(notificationTimer == 2000) {
				updateCoordinate();
				notificationTimer = 0;
			} else {
				notificationTimer++;
			}
			
		}
	}
	
	
	public static class Treasure extends NPC {
		int notificationTimer = 0;
		public Treasure(int id, int x, int y) {
			super(id, x, y);
			tickSpeed = 150;
			maxHealth = health = 60;
			power = 1;
			money = 120;
			exp = 100;
			
			data.put("charX", "7");
			data.put("charY", "0");
		}
		
		public void tick() {
			if(timer()) return;
			int minDistance = -1;
			int distance = -1;
			int targetUserId = 0;
			
			Collection<User> col = DVServer.users.values();
			Iterator<User> itr = col.iterator();
			while (itr.hasNext()) {
				User u = itr.next();
				if(u.x < 64 && u.y < 64) continue;
				distance = (int) Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
				if(distance <= 32 && (minDistance > distance || minDistance == -1)) {
					minDistance = distance;
					targetUserId = u.id;
				}
			}
			
			if(targetUserId != 0) {
				synchronized (DVServer.users) {
					User target = DVServer.users.get(targetUserId);
					int newMoney = target.data.getInt("money") + money;
					target.data.put("money", newMoney);
					target.data.put("x", target.x);
					target.data.put("y", target.y);
					target.tcp.send("me|" + target.data.toString());
				}
				kill();
			}
			
			if(notificationTimer == 2000) {
				updateCoordinate();
				notificationTimer = 0;
			} else {
				notificationTimer++;
			}
			
		}
	}	

	public NPC(int id, int x, int y) {
		this.x = x;
		this.y = y;
		this.id = id;
		this.last.setLocation(x, y);
		data.put("id", id);
		data.put("x", x);
		data.put("y", y);
	}
	
	public void setSprite(int x, int y) {
		data.put("charX", x);
		data.put("charY", y);
	}

	public void tick() {}

	public boolean timer() {
		if(tickTimer++ != tickSpeed) {
			this.last.setLocation(x, y);
			return true;
		}
		tickTimer = 0;
		
		return false;
	}

	public void updateCoordinate() {
		wallCollise();
		
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > (DVServer.gameLimit.x - 1) * 32) x = (DVServer.gameLimit.x - 1) * 32;
		if(y > (DVServer.gameLimit.y - 1) * 32) y = (DVServer.gameLimit.y - 1) * 32;
		
		data.put("y", y);
		data.put("x", x);
		DVServer.udp.echo("npc_move|" + id + "|" + x + "|" + y, null);		
	}
	
	public void kill() {
		live = false;
		DVServer.tcp.echo("kill_npc|" + id, null);
	}

	public void getDamage(User u, int power) {
		health -= power;
		if(health < 0) health = 0;
		
		if(health == 0) {
			live = false;
			int sendMoney = money + r.nextInt(11) - 5;
			int sendExp = exp + r.nextInt(5) - 2;
			if(u != null) DVServer.sendReward(u, sendMoney, sendExp);
		} else {
			DVServer.udp.echo("npc_health|" + id + "|" + health + "|" + maxHealth, null);
		}
		
		data.put("maxHealth", maxHealth);
		data.put("health", health);
	}
	
	public void wallCollise() {
		synchronized (DVServer.walls) {
			Iterator<Point> itr = DVServer.walls.iterator();
			while (itr.hasNext()) {
				Point w = itr.next();
				Point wall = new Point(w.x, w.y);
				wall.x *= 32;
				wall.y *= 32;
				if ((x > wall.x && x < wall.x + 32) || (x + 32 < wall.x + 32 && x + 32 > wall.x)) {
					if (y + 32 > wall.y && last.y + 32 <= wall.y) {
						y = last.y;
					}
					if (y < wall.y + 32 && last.y >= wall.y + 32) {
						y = last.y;
					}
				}

				if ((y > wall.y && y < wall.y + 32) || (y + 32 < wall.y + 32 && y + 32 > wall.y)) {
					if (x + 32 > wall.x && last.x + 32 <= wall.x) {
						x = last.x;
					}
					if (x < wall.x + 32 && last.x >= wall.x + 32) {
						x = last.x;
					}
				}

				if (x == wall.x && ((y + 32 > wall.y && last.y + 32 <= wall.y) || (y < wall.y + 32 && last.y >= wall.y + 32))) {
					y = last.y;
				}
				if (y == wall.y && ((x + 32 > wall.x && last.x + 32 <= wall.x) || (x < wall.x + 32 && last.x >= wall.x + 32))) {
					x = last.x;
				}
			}
		}
	}
	
	static public NPC fromTypeID(int typeID, int npcId, int x, int y) {
		switch(typeID) {
		case Monster.RAT:
			return new Rat(npcId, x, y);
		case Monster.FOLLOWER_RAT:
			return new AggressiveRat(npcId, x, y);
		case Monster.SPEED_RAT:
			return new SpeedyRat(npcId, x, y);
		case Monster.ZOMBIE:
			return new Zombie(npcId, x, y);
		case Monster.ICE_MAGE:
			return new IceMage(npcId, x, y);
		case Monster.BULL:
			return new BullMan(npcId, x, y);
		case MONEY:
			return new Money(npcId, x, y);
		case Monster.GUARD:
			return new Guard(npcId, x, y);
		case TREASURE:
			return new Treasure(npcId, x, y);
		case HOSTAGE:
			return new Hostage(npcId, x, y);
		case Monster.HOLE:
			return new Hole(npcId, x, y);
		default: 
			return new Rat(npcId, x, y);
		}
	}
}
