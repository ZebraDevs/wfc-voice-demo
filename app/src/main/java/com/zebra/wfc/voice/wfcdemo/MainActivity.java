/*******************************************************************************
 * Copyright (c) 2015 Zebra Technologies
 * All Rights Reserved
 *
 * This material contains information which is proprietary and
 * confidential to Zebra Technologies.  It is disclosed to the customer solely for
 * the following purpose; namely, for the purpose of enabling the customer
 * to evaluate the Zebra Technologies products described herein or for the use of
 * the information for the operation of the product delivered to the
 * customer.  Customer is not to reproduce, copy, divulge or sell all or
 * any part thereof without the prior consent of an authorized
 * representative of Zebra Technologies.
 *
 *******************************************************************************/
package com.zebra.wfc.voice.wfcdemo;

import com.symbol.wfc.service.ConnectorCallback;
import com.symbol.wfc.service.WFCVoiceConnector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * MainActivity is a sample activity initializing WFCVoiceConnector instance and
 * displaying WFCVoice state and calls updates. It allows user to manage all
 * ongoing calls without WFCVoice UI
 *
 */
public class MainActivity extends Activity implements OnClickListener,
		OnEditorActionListener {

	protected static final String TAG = "WFCDemo";

	/*
	 * WFCVoiceService Connector to communicate with WFCVoice application
	 * running in "headless" mode
	 */
	WFCVoiceConnector voiceService;

	/*
	 * The last call data bundle received from plugin connector
	 */
	private Bundle callData;

	/*
	 * ToneGenerator for playing custom incoming call ringtone
	 */
	private ToneGenerator toneGenerator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.i(TAG, "onCreate");

		findViewById(R.id.callStatelayout).setVisibility(View.GONE);

		((EditText) findViewById(R.id.phone)).setOnEditorActionListener(this);
		findViewById(R.id.call).setOnClickListener(this);
		findViewById(R.id.dial).setOnClickListener(this);
		findViewById(R.id.buttonDND).setOnClickListener(this);
		findViewById(R.id.buttonEnd).setOnClickListener(this);
		findViewById(R.id.buttonSpeaker).setOnClickListener(this);
		findViewById(R.id.buttonMute).setOnClickListener(this);
		findViewById(R.id.buttonMore).setOnClickListener(this);
		findViewById(R.id.buttonAccept).setOnClickListener(this);
		findViewById(R.id.buttonReject).setOnClickListener(this);

		((TextView)findViewById(R.id.versionTV))
				.setText("Version: " + BuildConfig.VERSION_NAME +"."+BuildConfig.VERSION_CODE);

		// register receiver for notifications from WFCVoice
		// (optional)
		// This can be used when WFCVoiceConnector is not initialized
		registerReceiver(mMessageReceiver, new IntentFilter(
				"wfc.voice.PHONE_STATE"));

	}

	// broadcast receiver to get notifications from WFC Connect app about
	// call state changes. This is optional when
	// This can be used when WFCVoiceConnector is not initialized
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG,
					"Received PHONE_STATE from WFConnect state="
							+ intent.getStringExtra("state")
					+ " registration_state=" + intent.getStringExtra("registration_state")
					+ " number=" + intent.getStringExtra("number")
					+ " line_id=" + intent.getStringExtra("line_id")
					+ " line_extension=" + intent.getStringExtra("line_extension")
					+ " line_registered=" + intent.getBooleanExtra("line_registered", false)
					+ " suspended=" + intent.getBooleanExtra("suspended", false)
				 );
		}
	};

	/*
	 * ConnectorCallback will be used by WFCVoiceConnector object to notify this
	 * activity
	 */
	private ConnectorCallback mVoiceServiceCallback = new ConnectorCallback() {

		/*
		 * @see com.symbol.wfc.service.ConnectorCallback#onDisconnected()
		 */
		@Override
		public void onDisconnected() {
			Log.i(TAG, "onDisconnected");

			// update voice service status
			((TextView) findViewById(R.id.serviceStatus))
					.setText("SERVICE DISCONNECTED");
			((ImageView) findViewById(R.id.serviceImage))
					.setImageResource(R.drawable.ic_notify_not_connected);

			// hide call info bar
			findViewById(R.id.callStatelayout).setVisibility(View.GONE);

			Toast.makeText(MainActivity.this, "WFCVoice plugin disconnected",
					Toast.LENGTH_SHORT).show();
		}

		/*
		 * @see com.symbol.wfc.service.ConnectorCallback#onConnected()
		 */
		@Override
		public void onConnected() {
			Log.i(TAG, "onConnected");

			// show toast and expect the following onServiceStateUpdated() to
			// refresh status

			Toast.makeText(MainActivity.this, "WFCVoice plugin connected",
					Toast.LENGTH_SHORT).show();
		}

		/**
		 * Called when WFCVoice service status changes.
		 *
		 * @param data
		 *            contains a set of key/value parameters describing service
		 *            status
		 *
		 *            "extension" - registered extension<br>
		 *            "statusString" - string describing service status<br>
		 *            (Example.: "Registered as XXXX")<br>
		 *            "state" - int value indicating service status:<br>
		 *            0 - NOT REGISTERED<br>
		 *            1 - REGISTERED<br>
		 *            2 - IN CALL<br>
		 *            3 - DND mode<br>
		 *
		 */
		@Override
		public void onServiceStateUpdated(Bundle data) {
			Log.i(TAG, "onServiceStateUpdated data=" + data.toString());

			// refresh WFCVoice status on top blue bar

			((TextView) findViewById(R.id.serviceStatus)).setText(data
					.getString("statusString", "Unknown"));

			int state = data.getInt("state", 0);

			// 0 - not registered
			// 1 - registered
			// 2 - in call
			// 3 - SUSPENDED or DND mode
			switch (state) {

				case 0:
					((ImageView) findViewById(R.id.serviceImage))
							.setImageResource(R.drawable.ic_notify_not_connected);
					break;
				case 1:
					((ImageView) findViewById(R.id.serviceImage))
							.setImageResource(R.drawable.ic_notify_registered);
					break;
				case 2:
					((ImageView) findViewById(R.id.serviceImage))
							.setImageResource(R.drawable.ic_notify_active_call);
					break;
				case 3:
					((ImageView) findViewById(R.id.serviceImage))
							.setImageResource(R.drawable.ic_do_not_disturb);
					break;
			}

			findViewById(R.id.callStatelayout).setVisibility(
					state == 2 ? View.VISIBLE : View.GONE);
		}

		/**
		 * Called when a voice call state changes.
		 *
		 * @param data
		 *            contains a set of key/value parameters describing the call
		 *
		 *            "callId" - unique call ID<br>
		 *            "peer" - peer's extension (other party phone number) <br>
		 *            "extension" - own extension<br>
		 *            "statusString" - string describing call status<br>
		 *            (Example.: "Active Call")<br>
		 *            "speaker" - boolean indicating if speaker mode is ON<br>
		 *            "mute" - boolean indicating if mute mode is ON<br>
		 *            "start_time" - long value when the call has started (in
		 *            milliseconds)<br>
		 *            "state" - int value indicating call state:<br>
		 *            0 - ENDED<br>
		 *            1 - ACTIVE<br>
		 *            2 - INCOMING<br>
		 *            3 - CALLING<br>
		 *            4 - ONHOLD<br>
		 *            5 - INSETUP<br>
		 *
		 */
		@Override
		public void onCallStateUpdated(Bundle data) {
			Log.i(TAG, "onCallStateUpdated data=" + data.toString());

			// show call status bar
			findViewById(R.id.callStatelayout).setVisibility(View.VISIBLE);

			// preserve call data bundle
			callData = data;

			// stone tone player if active
			stopTone();

			// enable/disable call actions based on call status

			((TextView) findViewById(R.id.callInfo)).setText(data.getString(
					"peer", "Unknown"));
			((TextView) findViewById(R.id.callStatus)).setText(data.getString(
					"statusString", "Unknown Status"));

			((ToggleButton) findViewById(R.id.buttonSpeaker)).setChecked(data
					.getBoolean("speaker", false));
			((ToggleButton) findViewById(R.id.buttonMute)).setChecked(data
					.getBoolean("mute", false));

			findViewById(R.id.buttonEnd).setVisibility(View.GONE);
			findViewById(R.id.buttonSpeaker).setVisibility(View.GONE);
			findViewById(R.id.buttonMute).setVisibility(View.GONE);
			findViewById(R.id.buttonAccept).setVisibility(View.GONE);
			findViewById(R.id.buttonReject).setVisibility(View.GONE);
			findViewById(R.id.buttonMore).setVisibility(View.VISIBLE);
			findViewById(R.id.callControls).setVisibility(View.VISIBLE);

			// 0 - ENDED
			// 1 - ACTIVE
			// 2 - INCOMING
			// 3 - CALLING
			// 4 - ONHOLD
			// 5 - INSETUP
			switch (data.getInt("callState", 0)) {
				case 0: // 0 - ENDED
				default:
					findViewById(R.id.buttonMore).setVisibility(View.GONE);
					findViewById(R.id.callControls).setVisibility(View.GONE);

					// request status refresh in .5 seconds, when call is ended or
					// unknown
					findViewById(R.id.callStatelayout).postDelayed(new Runnable() {

						@Override
						public void run() {
							if (voiceService != null)
								voiceService.requestStatusUpdate();
						}
					}, 1000);

					break;
				case 1: // 1 - ACTIVE
					findViewById(R.id.buttonEnd).setVisibility(View.VISIBLE);
					findViewById(R.id.buttonSpeaker).setVisibility(View.VISIBLE);
					findViewById(R.id.buttonMute).setVisibility(View.VISIBLE);
					break;
				case 2: // 2 - INCOMING
					playTone();
					findViewById(R.id.buttonAccept).setVisibility(View.VISIBLE);
					findViewById(R.id.buttonReject).setVisibility(View.VISIBLE);
					break;
				case 3: // 3 - CALLING
					findViewById(R.id.buttonEnd).setVisibility(View.VISIBLE);
					break;
				case 4: // 4 - ONHOLD
					findViewById(R.id.buttonEnd).setVisibility(View.VISIBLE);
					break;
				case 5: // 5 - INSETUP
					findViewById(R.id.buttonEnd).setVisibility(View.VISIBLE);
					break;
			}

			// start call clock when call is active
			Chronometer clock = (Chronometer) findViewById(R.id.clockTV);
			if (data.getInt("callState", 0) == 1) { // ACTIVE CALL

				clock.setVisibility(View.VISIBLE);
				clock.setBase(SystemClock.elapsedRealtime()
						- (System.currentTimeMillis() - data.getLong(
						"start_time", System.currentTimeMillis())));
				clock.start();
			} else {
				clock.stop();
				clock.setVisibility(View.GONE);
			}

		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.i(TAG, "onDestroy");
		unregisterReceiver(this.mMessageReceiver);

	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "onResume");

		// send request to refresh service status when activity becomes visible
		voiceService.requestStatusUpdate();

	}

	@Override
	protected void onStart() {
		super.onStart();

		Log.i(TAG, "onStart");

		// initialize WFCVoiceConnector when activity starts

		voiceService = new WFCVoiceConnector(getApplicationContext(),
				mVoiceServiceCallback);

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");

		// stop tone if it was playing when activity is paused
		stopTone();
	}

	@Override
	protected void onStop() {
		super.onStop();

		Log.i(TAG, "onStop");

		// disconnect from WFCVoiceConnector when activity stops

		voiceService.disconnect();
	}

	// process all button clicks here

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			case R.id.call: // send a new call request to WFCVoice service
				callPhoneNumber();
				break;

			case R.id.dial: // open WFCVoice dialer
				Intent callIntent = new Intent("wfc.voice.ACTION_BUTTON",
						Uri.parse("tel:"));
				callIntent.putExtra("action", "dial");
				startActivity(callIntent);
				break;

		/*
		 * Send DND request
		 */

			case R.id.buttonDND:
				voiceService.requestToggleDND();
				break;

		/*
		 * the following are ongoing call buttons actions
		 */

			case R.id.buttonAccept:
				voiceService.requestCallAction("accept", callData);
				break;
			case R.id.buttonReject:
				voiceService.requestCallAction("reject", callData);
				break;
			case R.id.buttonEnd:
				voiceService.requestCallAction("end", callData);
				break;
			case R.id.buttonSpeaker:
				voiceService.requestCallAction("speaker", callData);
				break;
			case R.id.buttonMute:
				voiceService.requestCallAction("mute", callData);
				break;
			case R.id.buttonMore:
				voiceService.requestCallAction("more", callData);
				break;
		}
	}

	// send request (via Intent) to initiate new sip call
	private void callPhoneNumber() {
		String phone = ((EditText) findViewById(R.id.phone)).getText()
				.toString();

		// check if phone number is not empty
		if (!TextUtils.isEmpty(phone)) {

			// close soft keyboard
			((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(findViewById(R.id.phone)
							.getWindowToken(), 0);

			// Send intent to WFCVoice app
			// Note: this intent does not require WFCVoiceConnector to be
			// initialized and connected
			Intent callIntent = new Intent("wfc.voice.ACTION_BUTTON",
					Uri.parse("tel:"));
			callIntent.putExtra("action", "call");
			callIntent.putExtra("value", phone);
			startActivity(callIntent);

		}
	}

	// Listen for soft keyboard actions
	@Override
	public boolean onEditorAction(TextView p_v, int p_actionId, KeyEvent p_event) {

		if (p_actionId == EditorInfo.IME_ACTION_SEND) {
			callPhoneNumber();
			return true;
		}

		return false;
	}

	/*
	 * Start playing incoming call tone/notification
	 */
	private void playTone() {

		// stop previous tone if any
		stopTone();

		try {
			toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
		} catch (RuntimeException e) {
			e.printStackTrace();
			toneGenerator = null;
		}

		if (toneGenerator != null) {
			toneGenerator.startTone(ToneGenerator.TONE_SUP_CALL_WAITING);
		}
	}

	/*
	 * Stop and release tone player
	 */
	private void stopTone() {
		if (toneGenerator != null)
			toneGenerator.stopTone();

		toneGenerator = null;
	}
}
