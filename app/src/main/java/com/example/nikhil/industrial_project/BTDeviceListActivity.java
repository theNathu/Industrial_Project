// BTDeviceListActivity.java
// Alive Technologies
package com.example.nikhil.industrial_project;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This Activity lists any paired devices and devices detected in the area after
 * discovery. When a device is chosen by the user, the MAC address of the device
 * is sent back to the parent Activity in the result Intent.
 */
public class BTDeviceListActivity extends ActionBarActivity {
	private static final String TAG = "DeviceListActivity";

	private static final Integer REQUEST_ENABLE_BT = 3;

	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	public static String EXTRA_DEVICE_NAME = "device_name";

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private Button mScanButton;
	// private ArrayAdapter<String> mBTDevicesArrayAdapter;

	ArrayList<HashMap<String, String>> listBTDevices = new ArrayList<HashMap<String, String>>();
	private SimpleAdapter mBTDevicesSimpleAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);

		// Set up the action bar.
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.select_device);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		// Get the local Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		mBTDevicesSimpleAdapter = new SimpleAdapter(
				this, 
				listBTDevices, 
				R.layout.list_item, 
				new String[] { "title", "summary", "extra" },
				new int[] { R.id.title, R.id.summary, R.id.extra });

		// Find and set up the ListView for paired devices and newly discovered devices
		ListView listViewBTDevices = (ListView) findViewById(R.id.bt_devices);
		listViewBTDevices.setClickable(true);
		listViewBTDevices.setAdapter(mBTDevicesSimpleAdapter);
		listViewBTDevices.setOnItemClickListener(mDeviceClickListener);

		// Initialize the button to perform device discovery
		mScanButton = (Button) findViewById(R.id.button_scan);
		mScanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mScanButton.setEnabled(false);
				if (!mBtAdapter.isEnabled()) {
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
					return;
				}
				startBTDiscovery();
			}
		});

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				addItem(device.getName(), device.getAddress(), "Paired");
			}
		}
		mBTDevicesSimpleAdapter.notifyDataSetChanged();
	}

	private void addItem(String title, String summary, String extra) {
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("title", title);
		item.put("summary", summary);
		item.put("extra", extra);
		listBTDevices.add(item);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;

		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void startBTDiscovery() {
		Log.d(TAG, "startBTDiscovery()");
		
		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}

		listBTDevices.clear();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				addItem(device.getName(), device.getAddress(), "Paired");
			}
		}
		mBTDevicesSimpleAdapter.notifyDataSetChanged();

		// Request discover from BluetoothAdapter
		if (mBtAdapter.startDiscovery()) {
			// Indicate scanning in the title
			setProgressBarIndeterminateVisibility(true);
			getSupportActionBar().setTitle(R.string.scanning);
		} else {
			mScanButton.setEnabled(true);
		}
	}

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			// Cancel discovery because it's costly and we're about to connect
			mBtAdapter.cancelDiscovery();

			Map<String, String> data = (Map<String, String>) parent.getItemAtPosition(position);

			// Get the device MAC address, which is the last 17 chars in the View
			String btname = data.get("title");
			String btaddress = data.get("summary");

			// Create the result Intent and include the MAC address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_NAME, btname);
			intent.putExtra(EXTRA_DEVICE_ADDRESS, btaddress);

			// Set result and finish this Activity
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed already
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

					// TODO: Add check if device is already listed. Currently we
					// can get Device twice (one with bt name and same device without bt name)
					addItem(device.getName(), device.getAddress(), "Not Paired");
					mBTDevicesSimpleAdapter.notifyDataSetChanged();
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
                getSupportActionBar().setTitle(R.string.select_device);
				if (listBTDevices.isEmpty()) {
					String noDevices = getResources().getText(R.string.none_found).toString();
					addItem(noDevices, "", "");
				}
				mScanButton.setEnabled(true);
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			// Check if Bluetooth enabled rather than using resultCode because of Bug. 
			// See http://code.google.com/p/android/issues/detail?id=9013
			if (mBtAdapter.isEnabled()) {
				startBTDiscovery();
			} else {
				mScanButton.setEnabled(true);
			}
		}
	}
}
