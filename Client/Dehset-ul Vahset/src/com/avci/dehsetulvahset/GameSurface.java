package com.avci.dehsetulvahset;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.avci.dehsetulvahset.TCPSocket.TCPListener;
import com.avci.dehsetulvahset.UDPSocket.DataRecerviedListener;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;

class GameSurface extends SurfaceView implements Runnable, SurfaceHolder.Callback {
	public SurfaceHolder holder;
	public boolean running = false;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public Thread thread;
	public static Boolean paused = false;
	Context ctx;

	private final static int MAX_FPS = 50;
	private final static int MAX_FRAME_SKIPS = 5;
	private final static int FRAME_PERIOD = 1000 / MAX_FPS;

	long currentTime = 0;
	int frame = 0;
	int fps = 0;

	int stepSpeed = 2;
	int stepTimer = 0;
	int stepSize = 8;

	int attackTimer = 0;
	int attackSpeed = 15;

	Bitmap[][] charSprites = cut(R.drawable.chars2, 32, 32);
	Bitmap[][] npcSprites = cut(R.drawable.monsters1, 32, 32);
	Bitmap[][] floorSprites = cut(R.drawable.floors, 32, 32);
	Bitmap[][] effectsSprites = cut(R.drawable.effects, 32, 32);
	Bitmap logo;
	Bitmap targetBitmap;

	public Point me = new Point();
	public Point cameraLimit = new Point();
	public Point gameLimit = new Point();
	public Point cameraPosition = new Point();

	HashMap<String, String> my_data = new HashMap<String, String>();
	Bitmap my_skin;
	int my_id = 0;
	public int freezeTimer;
	private String selectedNPCID = "0";

	public HashMap<Integer, User> users = new HashMap<Integer, User>();
	public HashMap<Integer, NPC> npcs = new HashMap<Integer, NPC>();
	public HashSet<Point> walls = new HashSet<Point>();
	
	public HashSet<Point> hitEffects = new HashSet<Point>();

	UDPSocket udp;
	TCPSocket tcp;

	static GameSurface instance;

	boolean ready = false;
	private int damageTimer;

	Point joystick = new Point();
	Point attackButton = new Point();
	private float joystickR;
	private float attackButtonR;
	private int dx, dy;
	private int joyPointerIndex = -1;
	private float cameraMoveLimit;
	private float cameraMoveSpeed;
	private Bitmap mapBitmap;

	public GameSurface(Context context) {
		super(context);
		ctx = context;

		GameSurface.instance = this;

		setKeepScreenOn(true);

		running = true;
		thread = new Thread(this);

		holder = getHolder();
		holder.addCallback(this);

		cameraLimit.set(Game.width, Game.height);
		
		logo = bitmapResize(BitmapFactory.decodeResource(getResources(), R.drawable.dv), d2p(200), d2p(30));
		targetBitmap = bitmapResize(BitmapFactory.decodeResource(getResources(), R.drawable.target), d2p(32), d2p(32));

		joystickR = d2p(60);
		attackButtonR = d2p(40);
		joystick.set((int) (d2p(10) + joystickR), (int) (Game.height - d2p(10) - joystickR));
		attackButton.set((int) (Game.width - attackButtonR - d2p(20)), (int) (Game.height - d2p(20) - attackButtonR));

		cameraMoveLimit = d2p(100);
		cameraMoveSpeed = 5;
		
		udp = main.udp;
		tcp = main.tcp;

		new Thread() {
			public void run() {
				for (int i = 0; i < 10; i++) {
					if (ready) break;
					tcp.send("getGameData");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		tcp.setListener(new ServerGameTCPListener());
		udp.setListener(new ServerGameUDPListener());

		new Thread() {
			public void run() {
				while (true) {
					if (!running)
						return;
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					udp.send("ping");
				}
			}
		}.start();
	}

	@Override
	public synchronized boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_2_DOWN: {
			int i = 0;
			if (action == MotionEvent.ACTION_POINTER_2_DOWN)
				i = 1;
			final float x = ev.getX(i);
			final float y = ev.getY(i);
			
			if (!selectedNPCID.equals("0")) {
				tcp.send("spawn_npc|" + selectedNPCID + "|" + (int) ((x - d2p(cameraPosition.x)) / (Game.dpi / 160)) + "|" + (int) ((y - d2p(cameraPosition.y)) / (Game.dpi / 160)));
			}
			
			int joyDistance = (int) Math.sqrt(Math.pow(joystick.x - x, 2) + Math.pow(joystick.y - y, 2));
			int attackDistance = (int) Math.sqrt(Math.pow(attackButton.x - x, 2) + Math.pow(attackButton.y - y, 2));
			if (attackDistance < joystickR + (joystickR / 2) && freezeTimer == 0) {
				attack(me.x, me.y);
			} else if (joyDistance < joystickR + (joystickR / 2)) {
				joyPointerIndex = i;
			}
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			if(joyPointerIndex == -1) return false;
			final float x = ev.getX(joyPointerIndex);
			final float y = ev.getY(joyPointerIndex);
			if (freezeTimer == 0) {
				dx = (int) ((x - joystick.x) / 5);
				dy = (int) ((y - joystick.y) / 5);
				if (dx > 4)
					dx = 4;
				if (dy > 4)
					dy = 4;
				if (dx < -4)
					dx = -4;
				if (dy < -4)
					dy = -4;
			} else {
				dx = 0;
				dy = 0;
			}
			break;
		}

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			joyPointerIndex = -1;
			dx = dy = 0;
			break;
		}
		}

		return true;
	}

	private void attack(int x, int y) {
		int minDistance = -1;
		int distance = -1;
		int targetNpcId = 0;
		int power = Integer.parseInt(my_data.get("power"));

		synchronized (npcs) {
			for (Entry<Integer, NPC> entry : npcs.entrySet()) {
				int npcID = entry.getKey();
				NPC npc = entry.getValue();

				distance = (int) Math.sqrt(Math.pow(npc.x - x, 2) + Math.pow(npc.y - y, 2));
				if (distance <= 32 && (minDistance > distance || minDistance == -1)) {
					minDistance = distance;
					targetNpcId = npcID;
				}
			}
		}

		if (targetNpcId != 0) {
			udp.send("attack|" + targetNpcId + "|" + power);
			synchronized (hitEffects) {
				hitEffects.add(new Point(npcs.get(targetNpcId).x, npcs.get(targetNpcId).y));
			}
		}
	}

	private void update() {
		if (!ready)
			return;
		if (stepTimer++ == stepSpeed) {
			Point last = new Point(me);
			
			me.x += dx;
			me.y += dy;
			if (me.x > gameLimit.x - 32)
				me.x = gameLimit.x - 32;
			if (me.y > gameLimit.y - 32)
				me.y = gameLimit.y - 32;
			if (me.x < 0)
				me.x = 0;
			if (me.y < 0)
				me.y = 0;

			synchronized (walls) {
				Iterator<Point> itr = walls.iterator();
				while (itr.hasNext()) {
					Point wall = itr.next();

					if ((me.x > wall.x && me.x < wall.x + 32) || (me.x + 32 < wall.x + 32 && me.x + 32 > wall.x)) {
						if (me.y + 32 > wall.y && last.y + 32 <= wall.y) {
							me.y = last.y;
						}
						if (me.y < wall.y + 32 && last.y >= wall.y + 32) {
							me.y = last.y;
						}
					}

					if ((me.y > wall.y && me.y < wall.y + 32) || (me.y + 32 < wall.y + 32 && me.y + 32 > wall.y)) {
						if (me.x + 32 > wall.x && last.x + 32 <= wall.x) {
							me.x = last.x;
						}
						if (me.x < wall.x + 32 && last.x >= wall.x + 32) {
							me.x = last.x;
						}
					}

					if (me.x == wall.x && ((me.y + 32 > wall.y && last.y + 32 <= wall.y) || (me.y < wall.y + 32 && last.y >= wall.y + 32))) {
						me.y = last.y;
					}
					if (me.y == wall.y && ((me.x + 32 > wall.x && last.x + 32 <= wall.x) || (me.x < wall.x + 32 && last.x >= wall.x + 32))) {
						me.x = last.x;
					}
				}
			}

			if (!last.equals(me.x, me.y))
				udp.send("move|" + me.x + "|" + me.y);

			if (-d2p(cameraPosition.x) < d2p(gameLimit.x) - cameraLimit.x + d2p(64) && d2p(me.x) > cameraLimit.x - d2p(cameraPosition.x) - cameraMoveLimit) {
				if(d2p(me.x) > cameraLimit.x - d2p(cameraPosition.x)) {
					cameraPosition.x = (int) ((cameraLimit.x - d2p(me.x)) / (Game.dpi / 160));
				}
				
				cameraPosition.x -= cameraMoveSpeed;
			}
			if (-d2p(cameraPosition.y) < d2p(gameLimit.y) - cameraLimit.y + d2p(64) && d2p(me.y) > cameraLimit.y - d2p(cameraPosition.y) - cameraMoveLimit) {
				if(d2p(me.y) > cameraLimit.y - d2p(cameraPosition.y)) {
					cameraPosition.y = (int) ((cameraLimit.y - d2p(me.y)) / (Game.dpi / 160));
				}
				cameraPosition.y -= cameraMoveSpeed;
			}
			if (-cameraPosition.x > -64 && me.x < cameraMoveLimit - cameraPosition.x) {
				if(me.x < - cameraPosition.x) {
					cameraPosition.x = (int) (- me.x);
				}
				cameraPosition.x += cameraMoveSpeed;
			}
			if (-cameraPosition.y > -64 && me.y < cameraMoveLimit - cameraPosition.y) {
				if(me.y < - cameraPosition.y) {
					cameraPosition.y = (int) (- me.y);
				}
				cameraPosition.y += cameraMoveSpeed;
			}

			stepTimer = 0;
		}
	}

	public void draw(Canvas c) {
		Point tmpCP = new Point(cameraPosition);
		c.translate(d2p(tmpCP.x), d2p(tmpCP.y));

		c.drawColor(Color.BLACK);

		if (!ready)
			return;

		if (damageTimer > 0) {
			c.translate(new Random().nextInt(11) - 5, new Random().nextInt(11) - 5);
		}
		
		paint.setAlpha(255);
		if(mapBitmap != null) c.drawBitmap(mapBitmap, -d2p(32), -d2p(32), paint);

		paint.setTextSize(d2p(10));
		
		paint.setAlpha(255);
		synchronized (npcs) {
			Collection<NPC> col = npcs.values();
			Iterator<NPC> itr = col.iterator();
			while (itr.hasNext()) {
				NPC npc_data = itr.next();
				c.drawBitmap(npc_data.skin, d2p(npc_data.x), d2p(npc_data.y), paint);

				if (npc_data.maxHealth != 0 && npc_data.health != 0) {
					float maxHealth = npc_data.maxHealth;
					float health = npc_data.health;
					float percent = health / (maxHealth / 100);
					float bar_width = 0.32f * percent;
					paint.setColor(Color.RED);

					c.drawRect(d2p(npc_data.x), d2p(npc_data.y - 2), d2p(npc_data.x + bar_width), d2p(npc_data.y - 4), paint);
				}
			}
			paint.setAlpha(255);
		}

		synchronized (users) {
			Collection<User> col = users.values();
			Iterator<User> itr = col.iterator();
			while (itr.hasNext()) {
				User other_user = itr.next();
				if (other_user.damageTimer > 0) {
					other_user.damageTimer--;
					c.drawBitmap(other_user.skin, d2p(other_user.x + new Random().nextInt(11) - 5), d2p(other_user.y + new Random().nextInt(11) - 5), paint);
				} else {
					c.drawBitmap(other_user.skin, d2p(other_user.x), d2p(other_user.y), paint);
				}
				float char_centerX = (d2p(32) / 2) + d2p(other_user.x);
				float char_centerY = d2p(other_user.y) - d2p(3);
				float name_width = paint.measureText(other_user.name) / 2;
				paint.setColor(Color.BLACK);
				c.drawText(other_user.name, char_centerX - name_width + 1, char_centerY + 1, paint);
				paint.setColor(Color.WHITE);
				c.drawText(other_user.name, char_centerX - name_width, char_centerY, paint);
			}
		}

		paint.setAlpha(255);
		
		synchronized (hitEffects) {
			Iterator<Point> itr = hitEffects.iterator();
			while (itr.hasNext()) {
				Point hit = itr.next();
				c.drawBitmap(effectsSprites[0][8], d2p(hit.x), d2p(hit.y), paint);
				itr.remove();
			}
		}
		
		c.drawBitmap(my_skin, d2p(me.x), d2p(me.y), paint);
		float char_centerX = (d2p(32) / 2) + d2p(me.x);
		float char_centerY = d2p(me.y) - d2p(3);
		float name_width = paint.measureText(my_data.get("name")) / 2;
		paint.setColor(Color.BLACK);
		c.drawText(my_data.get("name"), char_centerX - name_width + 1, char_centerY + 1, paint);
		paint.setColor(Color.YELLOW);
		c.drawText(my_data.get("name"), char_centerX - name_width, char_centerY, paint);

		c.translate(-d2p(tmpCP.x), -d2p(tmpCP.y));

		paint.setTextSize(d2p(16));
		paint.setColor(Color.BLACK);
		paint.setAlpha(180);
		c.drawText(my_data.get("money") + " $", d2p(11), d2p(65), paint);
		c.drawText(my_data.get("exp") + " xp", d2p(11), d2p(45), paint);
		
		paint.setColor(Color.WHITE);
		paint.setAlpha(180);
		c.drawText(my_data.get("money") + " $", d2p(10), d2p(66), paint);
		c.drawText(my_data.get("exp") + " xp", d2p(10), d2p(46), paint);

		paint.setTextSize(d2p(10));
		float ver_lenght = paint.measureText(main.app_ver);
		c.drawText(main.app_ver, Game.width - ver_lenght - d2p(3), d2p(35), paint);
		
		c.drawRect(d2p(10), d2p(10), d2p(160), d2p(26), paint);

		float max_health = Integer.parseInt(my_data.get("maxHealth"));
		float health = Integer.parseInt(my_data.get("health"));
		float percent = health / (max_health / 100);
		int health_bar_width = (int) d2p(1.44f * percent);

		paint.setColor(Color.RED);
		paint.setAlpha(180);
		if (health != 0)
			c.drawRect(d2p(13), d2p(13), health_bar_width + d2p(13), d2p(23), paint);
		
		paint.setColor(Color.WHITE);
		paint.setAlpha(180);
		String health_text = (int) health + "/" + (int) max_health;
		float health_lenght = paint.measureText(health_text);
		c.drawText(health_text, d2p(85) - (health_lenght / 2), d2p(21), paint);

		paint.setAlpha(150);
		c.drawBitmap(logo, Game.width - d2p(200), 0, paint);

		paint.setColor(Color.BLACK);
		paint.setAlpha(100);
		c.drawCircle(d2p(10) + joystickR, Game.height - d2p(10) - joystickR, joystickR, paint);

		paint.setColor(Color.RED);
		paint.setAlpha(100);
		c.drawCircle(attackButton.x, attackButton.y, attackButtonR, paint);
		String attack_text = "Saldır!";
		paint.setTextSize(d2p(18));
		paint.setShadowLayer(2, 0, 0, Color.BLACK);
		paint.setFakeBoldText(true);
		float attack_lenght = paint.measureText(attack_text);
		paint.setColor(Color.WHITE);
		paint.setAlpha(180);
		c.drawText(attack_text, attackButton.x - (attack_lenght / 2), attackButton.y+d2p(5), paint);
		paint.setColor(Color.RED);
		paint.setAlpha(100);
		paint.setFakeBoldText(false);
		paint.setShadowLayer(0, 0, 0, Color.WHITE);
		c.drawCircle(joystick.x + (dx * 10), joystick.y + (dy * 10), d2p(15), paint);

		if (damageTimer > 0) {
			damageTimer--;
			paint.setColor(damageTimer % 2 == 0 ? Color.RED : Color.BLACK);
			paint.setAlpha(60);
			c.drawRect(0, 0, Game.width, Game.height, paint);
		}

		if (freezeTimer > 0) {
			freezeTimer--;
			paint.setColor(Color.CYAN);
			paint.setAlpha(100);
			c.drawRect(0, 0, Game.width, Game.height, paint);
		}
	}

	private void getDamage() {
		damageTimer = 5;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (thread.getState() == Thread.State.NEW) {
			thread.start();
		} else if (thread.getState() == Thread.State.TERMINATED) {
			thread = new Thread(this);
			running = true;
			thread.start();
		}
	}

	public void run() {
		long beginTime;
		long timeDiff;
		int sleepTime;
		int framesSkipped;

		Canvas c;
		while (running) {
			c = null;
			if (!holder.getSurface().isValid())
				continue;
			try {
				c = holder.lockCanvas(null);

				synchronized (holder) {
					beginTime = System.currentTimeMillis();
					framesSkipped = 0;

					this.update();
					this.draw(c);

					timeDiff = System.currentTimeMillis() - beginTime;
					sleepTime = (int) (FRAME_PERIOD - timeDiff);

					if (sleepTime > 0) {
						try {
							Thread.sleep(sleepTime);
						} catch (InterruptedException e) {
						}
					}

					while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
						this.update();
						sleepTime += FRAME_PERIOD;
						framesSkipped++;
					}
				}
			} finally {
				if (c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
		boolean retry = true;
		running = false;

		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void drawMap(String[] map) {
		walls.removeAll(walls);
		
		float bgTileWidth = d2p(32);
		float bgTileHeight = d2p(32);
		float left = -bgTileWidth, top = -bgTileHeight;
		int index = 1;
		mapBitmap = Bitmap.createBitmap((int) d2p(gameLimit.x + 64), (int) d2p(gameLimit.y + 64), Bitmap.Config.ARGB_8888);
		Canvas mapCanvas = new Canvas(mapBitmap);
		while (left <= d2p(gameLimit.x)) {
			while (top <= d2p(gameLimit.y)) {
				if(top == -bgTileWidth || left == -bgTileWidth || top == d2p(gameLimit.y) || left == d2p(gameLimit.x)) {
					mapCanvas.drawBitmap(floorSprites[2][7], left + bgTileWidth, top + bgTileWidth, null);
					top += bgTileHeight;
					continue;
				}
				
				int code = Integer.parseInt(map[index]);
				code = (code == 255 ? 10 : code);
				int spriteX = code % floorSprites.length;
				int spriteY = (int) Math.floor(code / floorSprites.length);
				
				if(code == 70) {
					addWall((int) (left/bgTileWidth), (int) (top/bgTileHeight));
				}
				mapCanvas.drawBitmap(floorSprites[spriteX][spriteY], left + bgTileWidth, top + bgTileWidth, null);
				index++;
				top += bgTileHeight;
			}
			left += bgTileWidth;
			top = -bgTileWidth;
		}
		
		ready = true;
	}

	private Bitmap[][] cut(int resId, int bx, int by) {
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);

		int xTiles = bitmap.getWidth() / bx;
		int yTiles = bitmap.getHeight() / by;

		Bitmap[][] result = new Bitmap[xTiles][yTiles];

		for (int x = 0; x < xTiles; x++) {
			for (int y = 0; y < yTiles; y++) {
				result[x][y] = bitmapResize(Bitmap.createBitmap(bitmap, x * bx, y * by, bx, by), d2p(bx), d2p(by));
			}
		}

		return result;
	}

	private Bitmap bitmapResize(Bitmap bitmap, float newWidth, float newHeight) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		float scaleWidth = (float) newWidth / width;
		float scaleHeight = (float) newHeight / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}

	public float d2p(float dp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

	public void addWall(int x, int y) {
		walls.add(new Point(x * 32, y * 32));
	}

	/**********************************/

	public class ServerGameTCPListener implements TCPListener {
		@Override
		public void dataRecervied(String data) {
			Log.e("TCP " + data.length(), data);

			String[] datas = data.split("\\|");

			if (datas[0].equals("me") && datas.length == 2) {
				try {
					JSONObject jo = new JSONObject(datas[1]);
					@SuppressWarnings("unchecked")
					// Using legacy API
					Iterator<String> i = jo.keys();

					while (i.hasNext()) {
						String name = i.next();
						String value = jo.getString(name);
						my_data.put(name, value);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				int charX = my_data.get("charX") == null ? 0 : Integer.parseInt(my_data.get("charX"));
				int charY = my_data.get("charY") == null ? 0 : Integer.parseInt(my_data.get("charY"));
				freezeTimer = my_data.get("freezeTimer") == null ? 0 : Integer.parseInt(my_data.get("freezeTimer"));

				my_id = Integer.parseInt(my_data.get("id"));
				my_skin = charSprites[charX][charY];
				me.set(Integer.parseInt(my_data.get("x")), Integer.parseInt(my_data.get("y")));

			} else if (datas[0].equals("user") && datas.length > 1) {
				for (int l = 1; l < datas.length; l++) {
					HashMap<String, String> other_data = new HashMap<String, String>();
					JSONObject jo;
					try {
						jo = new JSONObject(datas[l]);
						@SuppressWarnings("unchecked")
						Iterator<String> i = jo.keys();

						while (i.hasNext()) {
							String name = i.next();
							String value = jo.getString(name);
							other_data.put(name, value);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

					int other_id = Integer.parseInt(other_data.get("id"));
					synchronized (users) {
						User other_user;
						if (users.get(other_id) == null) {
							other_user = new User(other_data);
							users.put(other_id, other_user);
						} else {
							other_user = users.get(other_id);
						}
						other_user.setData(other_data);
					}
				}
			} else if (datas[0].equals("game_limit")) {
				int x = Integer.parseInt(datas[1]);
				int y = Integer.parseInt(datas[2]);
				gameLimit.set(x*32, y*32);
			} else if (datas[0].equals("map")) {
				drawMap(datas);
			} else if (data.equals("userListComplete")) {
				Game.instance.hideLoading();
				return;
			} else if (datas[0].equals("npc") && datas.length > 1) {
				for (int l = 1; l < datas.length; l++) {
					HashMap<String, String> npc_data = new HashMap<String, String>();
					JSONObject jo;
					try {
						jo = new JSONObject(datas[l]);
						@SuppressWarnings("unchecked")
						Iterator<String> i = jo.keys();

						while (i.hasNext()) {
							String name = i.next();
							String value = jo.getString(name);
							npc_data.put(name, value);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

					int npc_id = Integer.parseInt(npc_data.get("id"));
					synchronized (npcs) {
						NPC npc = new NPC(npc_data);
						npcs.put(npc_id, npc);
					}
				}
			} else if (datas[0].equals("quit") && datas.length == 2) {
				int id = Integer.parseInt(datas[1]);
				synchronized (users) {
					if (users.get(id) != null) {
						users.remove(id);
					}
				}
			} else if (datas[0].equals("kill_npc") && datas.length == 2) {
				int id = Integer.parseInt(datas[1]);
				synchronized (npcs) {
					if (npcs.get(id) != null) {
						npcs.remove(id);
					}
				}
			} else if (datas[0].equals("get_reward") && datas.length == 3) {
				int money = Integer.parseInt(datas[1]) + Integer.parseInt(my_data.get("money"));
				int exp = Integer.parseInt(datas[2]) + Integer.parseInt(my_data.get("exp"));

				my_data.put("money", "" + money);
				my_data.put("exp", "" + exp);
			}

		}

		@Override
		public void connected() {

		}

		@Override
		public void disconnected() {
			main.instance.makeToast("Sunucu ile bağlantı kesildi.");
			Game.instance.finish();
			main.instance.finish();
		}
	}

	public class ServerGameUDPListener implements DataRecerviedListener {
		@Override
		public void DataRecervied(String data) {
			if (!ready)
				return;
			String[] datas = data.split("\\|");

			if (datas[0].equals("move") && datas.length == 4) {
				int id = Integer.parseInt(datas[1]);
				if (users.get(id) == null) {
					tcp.send("user_info|" + id);
					return;
				}

				User u = users.get(id);
				u.x = Integer.parseInt(datas[2]);
				u.y = Integer.parseInt(datas[3]);
			} else if (datas[0].equals("npc_move") && datas.length == 4) {
				int id = Integer.parseInt(datas[1]);
				if (npcs.get(id) == null) {
					tcp.send("npc_info|" + id);
					return;
				}

				NPC n = npcs.get(id);
				n.x = Integer.parseInt(datas[2]);
				n.y = Integer.parseInt(datas[3]);
			} else if (datas[0].equals("get_damage") && datas.length == 4) {
				int userID = Integer.parseInt(datas[1]);
				int power = Integer.parseInt(datas[2]);
				int health = Integer.parseInt(datas[3]);

				if (userID == my_id) {
					if(me.x <= 64 && me.y <= 64) return;
					my_data.put("health", "" + health);
					synchronized (hitEffects) {
						hitEffects.add(new Point(me.x, me.y));
					}
					getDamage();
				} else {
					if (users.get(userID) != null) {
						User u = users.get(userID);
						if(u.x <= 64 && u.y <= 64) return;
						u.damageTimer = 10;

						synchronized (hitEffects) {
							hitEffects.add(new Point(u.x, u.y));
						}
					}
				}
			} else if (datas[0].equals("npc_health") && datas.length == 4) {
				int npcID = Integer.parseInt(datas[1]);
				int health = Integer.parseInt(datas[2]);
				int maxHealth = Integer.parseInt(datas[3]);

				if (npcs.get(npcID) == null) {
					tcp.send("npc_info|" + npcID);
					return;
				}
				synchronized (npcs) {
					NPC npc = npcs.get(npcID);
					npc.health = health;
					npc.maxHealth = maxHealth;
				}
			}
		}
	}

	public void menuCreate(Menu menu) {
		if (my_data.get("gm") != null && my_data.get("gm").equals("1")) {
			if (selectedNPCID.equals("0")) {
				menu.add(0, 0, 0, "NPC Ekle");
			} else {
				menu.add(0, 0, 0, "NPC ID Değitir");
				menu.add(0, 1, 0, "NPC Eklemeden Çık");
			}
		}
	}

	public void menuItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			askNPCID();
			break;
		case 1:
			selectedNPCID = "0";
			break;
		}
	}

	public void askNPCID() {
		Game.instance.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Builder alert = new AlertDialog.Builder(Game.instance);
				final EditText input = new EditText(Game.instance);
				input.setInputType(InputType.TYPE_CLASS_NUMBER);
				alert.setCancelable(false);
				alert.setView(input);
				alert.setTitle("Lütfen NPC ID girin");
				alert.setMessage("Oyuna gönderilecek NPC ID'sini girin");
				alert.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String inputName = input.getText().toString().trim();
						if (inputName.equals("")) {
							askNPCID();
							return;
						}
						selectedNPCID = inputName;
					}
				});
				alert.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectedNPCID = "0";
					}
				});
				alert.show();
			}
		});
	}
}