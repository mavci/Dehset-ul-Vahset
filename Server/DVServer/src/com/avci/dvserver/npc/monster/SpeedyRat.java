package com.avci.dvserver.npc.monster;

import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class SpeedyRat extends Monster {
	public SpeedyRat(int id, int x, int y) {
		super(id, x, y);
		tickSpeed = 130;
		maxHealth = health = 15;
		power = 1;
		setSprite(1, 4);
	}
	
	public void tick() {
		if(timer() && live) return;
		int minDistance = 100000;
		int targetUserId = 0;
		
		Collection<User> col = DVServer.users.values();
		Iterator<User> itr = col.iterator();
		while (itr.hasNext()) {
			User u = itr.next();
			if(u.x < 64 && u.y < 64) continue;
			int distance = (int) Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
			if(distance < 50 && minDistance > distance) {
				minDistance = distance;
				targetUserId = u.id;
			}
		}
		
		if(targetUserId == 0) {
			x += r.nextInt(11) - 5;
			y += r.nextInt(11) - 5;
		} else if(minDistance != -1 && minDistance <= 32) {
			DVServer.hitUser(targetUserId, power);
		} else {
			User target = DVServer.users.get(targetUserId);
			if(target.x < x) x-=10;
			if(target.y < y) y-=10;
			if(target.x > x) x+=10;
			if(target.y > y) y+=10;
		}

		x += r.nextInt(11) - 5;
		y += r.nextInt(11) - 5;
		
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > DVServer.gameLimit.x * 32) x = DVServer.gameLimit.x * 32;
		if(y > DVServer.gameLimit.y * 32) y = DVServer.gameLimit.y * 32;
		
		updateCoordinate();
	}
}