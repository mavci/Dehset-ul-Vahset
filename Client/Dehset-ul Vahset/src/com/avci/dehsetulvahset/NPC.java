package com.avci.dehsetulvahset;

import java.util.HashMap;

import android.graphics.Bitmap;

public class NPC {
	int x, y, health = 0, maxHealth = 0;
	Bitmap skin = null;
	HashMap<String, String> data = new HashMap<String, String>();
	
	public NPC(HashMap<String, String> d) {
		setData(d);
	}

	public void setData(HashMap<String, String> d) {
		int charX = d.get("charX") == null ? 0 : Integer.parseInt(d.get("charX"));
		int charY = d.get("charY") == null ? 0 : Integer.parseInt(d.get("charY"));
		x = Integer.parseInt(d.get("x"));
		y = Integer.parseInt(d.get("y"));
		maxHealth = d.get("maxHealth") == null ? 0 : Integer.parseInt(d.get("maxHealth"));
		health = d.get("health") == null ? 0 : Integer.parseInt(d.get("health"));
		this.skin = GameSurface.instance.npcSprites[charX][charY];
		this.data = d;
	}
}
