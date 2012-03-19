package com.avci.dvserver.npc;

import java.awt.Point;

import com.avci.dvserver.DVServer;
import com.avci.dvserver.User;

public class Hostage extends NPC {
	int followingUserId = 0;
	Point margin = new Point();
	public Hostage(int id, int x, int y) {
		super(id, x, y);
		tickSpeed = 100;
		maxHealth = health = 80;
		power = 0;
		money = 10;
		exp = 10;
		margin.setLocation(r.nextInt(25) + 25, r.nextInt(25) + 25);
		
		data.put("charX", "0");
		data.put("charY", "7");
	}
	
	public void tick() {
		if(timer()) return;
		
		Point new_position = new Point(x, y);
		
		if(new_position.x <= 32 && new_position.y <= 32) {
			synchronized (DVServer.users) {
				DVServer.sendReward(DVServer.users.get(followingUserId), money, exp);
			}
			live = false;
		} else if(new_position.x <= 64 && new_position.y <= 64) {
			new_position.x -= 5;
			new_position.y -= 5;
		} else if(followingUserId != 0) {
			synchronized (DVServer.users) {
				User target = DVServer.users.get(followingUserId);
				if(target == null) {
					followingUserId = 0;
				} else {
					if(x > target.x - margin.x) new_position.x -= 5;
					if(y > target.y - margin.y) new_position.y -= 5;
					if(x < target.x + margin.x) new_position.x += 5;
					if(y < target.y + margin.y) new_position.y += 5;
				}
			}
		}
		
		if(new_position.distance(x, y) != 0) {
			x = new_position.x;
			y = new_position.y;
			updateCoordinate();
		}
		
	}
	
	public void getDamage(User u, int power) {
		if(u == null) {
			super.getDamage(u, power);
			return;
		}
		
		if(x <= 64 && y <= 64)
			return;
		
		if(followingUserId == u.id) {
			followingUserId = 0;
		} else {
			followingUserId = u.id;
		}
	}
}
