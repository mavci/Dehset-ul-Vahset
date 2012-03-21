package com.avci.dehsetulvahset;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

public class Game extends Activity {
	private GameSurface gameSurface = null;
	ProgressDialog loadingDialog;
	Menu menu;
	static float dpi;
	public static Game instance;
	static Game activity;
	static SharedPreferences options;
	
	static int width;
	static int height;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Game.instance = this;
        
		loadingDialog = ProgressDialog.show(Game.this, "", "Oyun açılıyor ...", true);
		loadingDialog.show();
        
        initGame();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      initGame();
    }
    
    @Override
	protected void onResume() {
    	initGame();
		super.onResume();
	}
    
    private void initGame() {
    	activity = this;
        Game.options = PreferenceManager.getDefaultSharedPreferences(this);
        
        Display display = getWindowManager().getDefaultDisplay(); 
        width = display.getWidth();
		height = display.getHeight();
		DisplayMetrics dm = getResources().getDisplayMetrics();
		dpi = dm.densityDpi;
		
		if(gameSurface == null) {
	        gameSurface = new GameSurface(this);		
	        setContentView(gameSurface);			
		}
    }
    
    public void hideLoading() {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				loadingDialog.dismiss();
			}
		});
    }

	@Override
	protected void onPause() {
		super.onPause();
		GameSurface.paused = true;
	}
	

	@Override
	protected void onDestroy() {
		main.instance.finish();
		super.onDestroy();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		gameSurface.menuCreate(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		gameSurface.menuItemSelected(item);
		return super.onOptionsItemSelected(item);
	}
}
