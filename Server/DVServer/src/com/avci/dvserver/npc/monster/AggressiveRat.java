package com.avci.dvserver.npc.monster;

import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class AggressiveRat extends Monster {
	public AggressiveRat(int id, int x, int y) {
		super(id, x, y);
		power = 1;
		tickSpeed = 200;
		maxHealth = health = 60;
		setSprite(3, 4);
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
			if(distance < 100 && (minDistance > distance || minDistance == -1)) {
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
			if(target.x < x) x-=5;
			if(target.y < y) y-=5;
			if(target.x > x) x+=5;
			if(target.y > y) y+=5;
		}
		
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > DVServer.gameLimit.x * 32) x = DVServer.gameLimit.x * 32;
		if(y > DVServer.gameLimit.y * 32) y = DVServer.gameLimit.y * 32;
		
		updateCoordinate();
	}
}