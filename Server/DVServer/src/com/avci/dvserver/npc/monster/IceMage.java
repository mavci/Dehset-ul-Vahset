package com.avci.dvserver.npc.monster;

import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class IceMage extends Monster {
	int escapeTimer = 0;
	public IceMage(int id, int x, int y) {
		super(id, x, y);
		tickSpeed = 150;
		maxHealth = health = 50;
		power = 1;
		money = 12;
		exp = 8;
		setSprite(5, 0);
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
			if(distance < 200 && (minDistance > distance || minDistance == -1)) {
				minDistance = distance;
				targetUserId = u.id;
			}
		}
		
		if(targetUserId == 0) {
			x += r.nextInt(11) - 5;
			y += r.nextInt(11) - 5;
		} else if(minDistance != -1 && minDistance <= 80 && escapeTimer == 0) {
			DVServer.hitUser(targetUserId, power);
			User u = DVServer.users.get(targetUserId);
			u.data.put("freezeTimer", 250);
			u.data.put("x", u.x);
			u.data.put("y", u.y);
			u.tcp.send("me|" + u.data.toString());
			u.data.put("freezeTimer", 0);
			escapeTimer = 100;
		} else {
			User target = DVServer.users.get(targetUserId);
			int a = 1;
			if(escapeTimer > 0) {
				escapeTimer--;
				a = -1;
			}
			if(target.x < x) x-=5 * a;
			if(target.y < y) y-=5 * a;
			if(target.x > x) x+=5 * a;
			if(target.y > y) y+=5 * a;
			
			if(minDistance != -1 && minDistance <= 80) {
				DVServer.hitUser(targetUserId, power);				
			}
		}
		
		x += r.nextInt(7) - 3;
		y += r.nextInt(7) - 3;
		
		updateCoordinate();
	}
}