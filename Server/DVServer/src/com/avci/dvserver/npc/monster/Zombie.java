package com.avci.dvserver.npc.monster;

import java.util.Collection;
import java.util.Iterator;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class Zombie extends Monster {
	public Zombie(int id, int x, int y) {
		super(id, x, y);
		tickSpeed = 230;
		maxHealth = health = 200;
		power = 2;
		money = 8;
		exp = 4;
		setSprite(0, 0);
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
		} else if(minDistance != -1 && minDistance <= 32) {
			DVServer.hitUser(targetUserId, power);
		} else {
			User target = DVServer.users.get(targetUserId);
			if(target.x < x) x-=5;
			if(target.y < y) y-=5;
			if(target.x > x) x+=5;
			if(target.y > y) y+=5;
		}
		
		x += r.nextInt(7) - 3;
		y += r.nextInt(7) - 3;
		
		updateCoordinate();
	}
}