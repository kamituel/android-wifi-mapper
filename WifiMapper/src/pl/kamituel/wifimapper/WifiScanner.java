package pl.kamituel.wifimapper;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WifiScanner extends BroadcastReceiver  {
	private boolean mRunning;
	private WifiManager mWifiManager;
	private OnWifiScan mListener;
	
	public interface OnWifiScan {
		public void onWifiScan(List<ScanResult> result);
	}
	
	public WifiScanner(Context ctx, OnWifiScan listener) {
		mWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
		mWifiManager.setWifiEnabled(true);
		registerSelfAsWifiBroadcastReceiver(ctx);
		mListener = listener;
	}

	public void start() {
		mRunning = true;
		mWifiManager.startScan();
	}
	
	public void stop() {
		mRunning = false;
	}
	
	private void registerSelfAsWifiBroadcastReceiver(Context ctx) {
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		ctx.registerReceiver(this, filter);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mListener.onWifiScan(mWifiManager.getScanResults());
//		Iterator<ScanResult> resultsIt = mWifiManager.getScanResults().iterator();
//		
//		while (resultsIt.hasNext()) {
//			ScanResult result = resultsIt.next();
//			Log.d("xxx", result.BSSID + " -> " + result.level);
//		}
		
		if (mRunning) {
			mWifiManager.startScan();
		}
	}
}
