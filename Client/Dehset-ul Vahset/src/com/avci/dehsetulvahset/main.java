package com.avci.dehsetulvahset;

import java.util.UUID;

import com.avci.dehsetulvahset.TCPSocket.TCPListener;
import com.avci.dehsetulvahset.UDPSocket.DataRecerviedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity {
	String ServerIP = "127.0.0.1";
	int tcpPort = 6789;
	int udpPort = 9876;
	public static TCPSocket tcp;
	public static UDPSocket udp;
	private String deviceID;

	public int status = 0;
	public Button playButton;

	public String name = null;
	public String secret = null;
	public int id = 0;
	private TextView statusView;
	static String app_ver;
	
	public ProgressDialog loadingDialog;
	
	static main instance;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		main.instance = this;
		
		try {
			app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
		
		TextView app_verView = (TextView) findViewById(R.id.app_ver);
		app_verView.setText(app_ver);
		
		// Telefonun ID tespit kısmı
		final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		deviceID = deviceUuid.toString();

		statusView = (TextView) findViewById(R.id.status);
		playButton = (Button) findViewById(R.id.playButton);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playButton.setEnabled(false);
				status = 10;
				new Thread() {
					public void run() {
						for(int i = 0; i<10; i++) {
							if(status != 10) break;
							udp.send("connect|" + id + "|" + secret);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								playButton.setEnabled(true);
							}
						});
					}
				}.start();
			}
		});
		
		
		loadingDialog = ProgressDialog.show(main.this, "", "Sunucuya bağlanıyor ...", true);
		loadingDialog.show();

		tcp = new TCPSocket(ServerIP, tcpPort);
		tcp.setListener(new ServerTCPListener());
		tcp.connect();

		udp = new UDPSocket(ServerIP, udpPort);
		udp.setListener(new ServerUDPListener());
	}

	public void askName() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Builder alert = new AlertDialog.Builder(main.this);
				final EditText input = new EditText(main.this);

				InputFilter[] FilterArray = new InputFilter[1];
				FilterArray[0] = new InputFilter.LengthFilter(16);
				input.setFilters(FilterArray);
				alert.setCancelable(false);
				alert.setView(input);
				alert.setTitle("Lütfen isminizi girin");
				alert.setMessage("Oyundaki karakteriniz için bir isim girin:");
				alert.setPositiveButton("Tamam", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String inputName = input.getText().toString().trim();
						if (inputName.equals("")) {
							askName();
							return;
						}
						
						name = inputName;
						tcp.send("name|" + inputName);
					}
				});
				alert.show();
			}
		});
	}

	public class ServerTCPListener implements TCPListener {
		@Override
		public void dataRecervied(String data) {
			Log.e("TCP", data);
			String[] datas = data.split("\\|");

			if (datas[0].equals("welcome") && datas.length == 5) {
				status = 2;
				name = datas[2];
				secret = datas[3];
				final String online = datas[4];
				id = Integer.parseInt(datas[1]);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						playButton.setEnabled(true);
						statusView.setText("Oyuna hoşgeldin " + name + "! (" + online + " online)");
					}
				});
			} else if (datas[0].equals("name")) {
				askName();
			} else if (datas[0].equals("name_exists")) {
				makeToast("Bu isim başkası tarafından alınmış, lütfen başka bir isim deneyin.");
				askName();
			}
		}

		@Override
		public void connected() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(loadingDialog.isShowing())
						loadingDialog.dismiss();
				}
			});

			status = 1;
			tcp.send("key|" + deviceID);
		}

		@Override
		public void disconnected() {
			makeToast("Sunucu ile bağlantı kesildi.");
			finish();
		}
	}
	
	public class ServerUDPListener implements DataRecerviedListener {
		@Override
		public void DataRecervied(String data) {
			Log.e("UDP", data);
			
			if(data.equals("ok") && status == 10) {
				status = 11;
				startActivity(new Intent(main.this, Game.class));
			} else if(data.equals("error") || data.equals("no")){
				makeToast("Oyun sunucuna bağlanırken bir sorun yaşandı. Lütfen tekar deneyin.");
				finish();
			}
		}
	}

	@Override
	protected void onDestroy() {
		tcp.send("bye");
		tcp.close();
		System.gc();
		super.onDestroy();
	}
	
	public void makeToast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(main.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}
}