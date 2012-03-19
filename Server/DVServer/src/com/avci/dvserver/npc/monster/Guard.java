package com.avci.dvserver.npc.monster;

import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;
import com.avci.dvserver.npc.Hostage;
import com.avci.dvserver.npc.NPC;

public class Guard extends Monster {
	Point target = new Point();
	public Guard(int id, int x, int y) {
		super(id, x, y);
		this.tickSpeed = 150;
		maxHealth = health = 90;
		power = 8;
		target.setLocation(x, y);
		setSprite(5, 14);
	}
	
	public void tick() {
		if(timer()) return;
		int minDistance = -1;
		int targetUserId = 0;
		boolean is_hostage = false;
		
		synchronized (DVServer.npcs) {
			Collection<NPC> col1 = DVServer.npcs.values();
			Iterator<NPC> itr1 = col1.iterator();
			while (itr1.hasNext()) {
				NPC n = itr1.next();
				if(n.equals(this)) continue;
				if(!(n instanceof Hostage)) continue;
				int targetDistance = (int) Math.sqrt(Math.pow(n.x - target.x, 2) + Math.pow(n.y - target.y, 2));
				if(targetDistance < 100 && (minDistance > targetDistance || minDistance == -1)) {
					minDistance = targetDistance;
					targetUserId = n.id;
					is_hostage = true;
				}
			}
		}
		
		if(!is_hostage) {
			synchronized (DVServer.users) {
				Collection<User> col = DVServer.users.values();
				Iterator<User> itr = col.iterator();
				while (itr.hasNext()) {
					User u = itr.next();
					if(u.x < 64 && u.y < 64) continue;
					int targetDistance = (int) Math.sqrt(Math.pow(u.x - target.x, 2) + Math.pow(u.y - target.y, 2));
					if(targetDistance < 100 && (minDistance > targetDistance || minDistance == -1)) {
						minDistance = targetDistance;
						targetUserId = u.id;
					}
				}
			}
		}
		
		if(targetUserId == 0) {
			if(target.x > x) x+=5;
			if(target.y > y) y+=5;
			if(target.x < x) x-=5;
			if(target.y < y) y-=5;
		} else {
			Point victim = new Point();
			if(is_hostage) {
				synchronized (DVServer.npcs) {
					NPC n = DVServer.npcs.get(targetUserId);
					victim.setLocation(n.x, n.y);
				}
			} else {
				synchronized (DVServer.users) {
					User u = DVServer.users.get(targetUserId);
					victim.setLocation(u.x, u.y);
				}
			}
			if(victim.x > x) x+=5;
			if(victim.y > y) y+=5;
			if(victim.x < x) x-=5;
			if(victim.y < y) y-=5;
		}
		
		if(targetUserId != 0) {
			if(is_hostage) {
				synchronized (DVServer.npcs) {
					int distance = (int) Math.sqrt(Math.pow(DVServer.npcs.get(targetUserId).x - x, 2) + Math.pow(DVServer.npcs.get(targetUserId).y - y, 2));
					if(distance <= 32) {
						DVServer.npcs.get(targetUserId).getDamage(null, power);
					}
				}
			} else {
				synchronized (DVServer.users) {
					int distance = (int) Math.sqrt(Math.pow(DVServer.users.get(targetUserId).x - x, 2) + Math.pow(DVServer.users.get(targetUserId).y - y, 2));
					if(distance <= 32) {
						DVServer.hitUser(targetUserId, power);
					}
				}
			}
		}
		
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > DVServer.gameLimit.x * 32) x = DVServer.gameLimit.x * 32;
		if(y > DVServer.gameLimit.y * 32) y = DVServer.gameLimit.y * 32;
		
		updateCoordinate();
	}
}