package com.avci.dvserver.npc.monster;
import com.avci.dvserver.npc.NPC;

public class Monster extends NPC {
	public Monster(int id, int x, int y) {
		super(id, x, y);
	}

	public static final int RAT = 1;
	public static final int FOLLOWER_RAT = 2;
	public static final int SPEED_RAT = 3;
	public static final int ZOMBIE = 4;
	public static final int ICE_MAGE = 5;
	public static final int BULL = 6;
	public static final int GUARD = 8;
	public static final int HOLE = 11;
}
