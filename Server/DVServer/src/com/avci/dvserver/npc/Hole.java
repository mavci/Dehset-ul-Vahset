package com.avci.dvserver.npc;

import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class Hole extends NPC {
	Point target = new Point();
	public Hole(int id, int x, int y) {
		super(id, x, y);
		this.tickSpeed = 200;
		maxHealth = health = 1;
		power = 999999;
		target.setLocation(x, y);
		setSprite(7, 5);
	}
	
	public void tick() {
		if(timer()) return;
		int minDistance = -1;
		int targetUserId = 0;
		
		synchronized (DVServer.users) {
			Collection<User> col = DVServer.users.values();
			Iterator<User> itr = col.iterator();
			while (itr.hasNext()) {
				User u = itr.next();
				if(u.x < 64 && u.y < 64) continue;
				int targetDistance = (int) Math.sqrt(Math.pow(u.x - target.x, 2) + Math.pow(u.y - target.y, 2));
				if(targetDistance <= 32 && (minDistance > targetDistance || minDistance == -1)) {
					minDistance = targetDistance;
					targetUserId = u.id;
				}
			}
		}
		
		if(targetUserId != 0) {
			DVServer.hitUser(targetUserId, power);
		}
		
		updateCoordinate();
	}
	
	public void getDamage(User u, int power) {
		if(u.data.getInt("gm") == 1) super.getDamage(u, power);
		return;
	}
}
