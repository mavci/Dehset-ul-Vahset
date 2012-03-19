package com.avci.dehsetulvahset;

import java.util.HashMap;

import android.graphics.Bitmap;

public class User {
	int x, y;
	String name = null;
	Bitmap skin = null;
	HashMap<String, String> data = new HashMap<String, String>();
	public int damageTimer;
	
	public User(HashMap<String, String> d) {
		setData(d);
	}

	public void setData(HashMap<String, String> d) {
		this.name = d.get("name");
		int charX = d.get("charX") == null ? 0 : Integer.parseInt(d.get("charX"));
		int charY = d.get("charY") == null ? 0 : Integer.parseInt(d.get("charY"));
		x = Integer.parseInt(d.get("x"));
		y = Integer.parseInt(d.get("y"));
		this.skin = GameSurface.instance.charSprites[charX][charY];
		this.data = d;
	}
}
