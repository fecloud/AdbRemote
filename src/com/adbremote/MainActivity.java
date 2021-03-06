/**
 * @(#) MainActivity.java Created on 2015-8-10
 *
 * 
 */
package com.adbremote;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * The class <code>MainActivity</code>
 * 
 * @author Feng OuYang
 * @version 1.0
 */
public class MainActivity extends Activity implements OnClickListener {

	private TextView textView, ip;

	private AdbRemoteTask adbRemoteTask;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.main);
		super.onCreate(savedInstanceState);

		findViewById(R.id.button).setOnClickListener(this);
		textView = (TextView) findViewById(R.id.textView);
		ip = (TextView) findViewById(R.id.ip);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		if (adbRemoteTask == null) {
			adbRemoteTask = new AdbRemoteTask();
			adbRemoteTask.execute();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		getActiveNetwork();
		super.onResume();
	}

	private void getActiveNetwork() {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (wifiManager.isWifiEnabled()) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if (wifiInfo != null) {
				int ipAddress = wifiInfo.getIpAddress();
				String ip = intToIp(ipAddress);
				this.ip.setText("ip address "+ip);
			}
		} else {
			this.ip.setText(R.string.openwifi);
		}

	}

	private String intToIp(int i) {

		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

	private class AdbRemoteTask extends AsyncTask<String, String, Boolean> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			textView.setText("");
			super.onPreExecute();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Boolean doInBackground(String... params) {
			try {
				final Process exec = Runtime.getRuntime().exec("su");

				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(exec.getInputStream()));

				final DataOutputStream out = new DataOutputStream(
						exec.getOutputStream());
				out.write("id\n".getBytes());
				out.flush();

				final String readLine = reader.readLine();
				if (readLine.startsWith("uid=0")) {
					publishProgress(getString(R.string.get_su_ok));

					out.write("setprop service.adb.tcp.port 5555\n".getBytes());
					out.flush();
					publishProgress(getString(R.string.settings_wifi_debug));

					out.write("stop adbd\n".getBytes());
					out.flush();
					publishProgress(getString(R.string.stop_adbd));

					out.write("start adbd\n".getBytes());
					out.flush();
					publishProgress(getString(R.string.start_adbd));
				}

				out.write("exit\n".getBytes());
				out.flush();

				exec.waitFor();

				out.close();
				reader.close();
				publishProgress(getString(R.string.finish));

			} catch (Exception e) {
				publishProgress(getString(R.string.get_su_fail));
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(String... values) {
			final String text = textView.getText().toString();
			final StringBuilder builder = new StringBuilder(text);
			builder.append("\n");
			builder.append(values[0]);
			textView.setText(builder);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			adbRemoteTask = null;
		}

	}

}
