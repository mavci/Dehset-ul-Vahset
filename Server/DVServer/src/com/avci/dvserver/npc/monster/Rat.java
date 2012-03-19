package com.avci.dvserver.npc.monster;

import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class Rat extends Monster {
	int targetX, targetY;
	int targetSpeed = 30;
	int targetTimer = 0;
	public Rat(int id, int x, int y) {
		super(id, x, y);
		this.tickSpeed = 180;
		maxHealth = health = 20;
		targetX = x;
		targetY = y;
		setSprite(0, 4);
	}
	
	public void tick() {
		if(timer()) return;
		int minDistance = -1;
		int targetUserId = 0;
		
		Collection<User> col = DVServer.users.values();
		Iterator<User> itr = col.iterator();
		while (itr.hasNext()) {
			User u = itr.next();
			if(u.x < 64 && u.y < 64) continue;
			int distance = (int) Math.sqrt(Math.pow(u.x - x, 2) + Math.pow(u.y - y, 2));
			if(distance < 100 && (minDistance > distance || minDistance == -1)) {
				minDistance = distance;
				targetUserId = u.id;
			}
		}
		
		if(targetUserId == 0) {
			if(targetX > x) x+=5;
			if(targetY > y) y+=5;
			if(targetX < x) x-=5;
			if(targetY < y) y-=5;
		} else {
			User victim = DVServer.users.get(targetUserId);
			if(victim.x < x) x+=5;
			if(victim.y < y) y+=5;
			if(victim.x > x) x-=5;
			if(victim.y > y) y-=5;
			targetX = x;
			targetY = y;
		}
		
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > DVServer.gameLimit.x * 32) x = DVServer.gameLimit.x * 32;
		if(y > DVServer.gameLimit.y * 32) y = DVServer.gameLimit.y * 32;
		
		if(targetTimer++ == targetSpeed) {
			int tx = Math.round((r.nextInt(300) - 150 + x) / 5) * 5;
			int ty = Math.round((r.nextInt(300) - 150 + y) / 5) * 5;
			targetX = tx;
			targetY = ty;
			targetTimer = 0;
		}

		updateCoordinate();
	}
}