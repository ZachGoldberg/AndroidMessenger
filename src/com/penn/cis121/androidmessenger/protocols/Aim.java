/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger.protocols;


import java.util.Map;
import java.util.logging.Logger;

import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMClient;
import com.levelonelabs.aim.AIMListener;
import com.penn.cis121.androidmessenger.AMProtocolMapper;
import com.penn.cis121.androidmessenger.AMService;


/*
 * All Client Classes require the following:
 * extends service and provides an AMService IBinder with all necessary functions implemented
 * Sends the following Intents to action "AndroidMessenger"
 * All intents specify their "purpose" via an extra called "purpose"
 * Format: purpose - other extras, description
 * connected - username, password, className - tell the messenger that we've successfully connected 
 */

public class Aim extends Service implements AIMListener {
	private boolean connected = false;
	private String username;
	private String password;
	private int connectionId;
	
	private AIMClient aim;
	static Logger logger = Logger.getLogger(AIMClient.class.getName());
	
	static {
		AMProtocolMapper.addMapping(Aim.class,"AIM");
	}
		
	/* Service Functions */
	@Override
	public IBinder onBind(Intent arg0) {
		AMService.Stub mBinder = new AMService.Stub() {
			public void Connect(String username, String password, int connectionId)
					throws DeadObjectException {
				// ConnectAIM("AndroidTest","q1w2e3r4");
				ConnectAIM(username, password,connectionId);
			}
			public void sendMessage(String buddyName, String message) throws DeadObjectException {
				aim.sendMessage(aim.getBuddy(buddyName), message);				
			}		
		};
		return mBinder;
		
	}	
	
	@Override
	protected void onCreate(){
		super.onCreate();
	}
	@Override
	protected void onDestroy(){
		if(connected && aim != null)
			aim.signOff();
		super.onDestroy();
	}
	public void ConnectAIM(String username, String password,int connectionId) {
		this.username = username;
		this.password = password;
		this.connectionId = connectionId;
		
		aim = new AIMClient(username, password,"",true);
		// Third option is for profiles; we don't support that for now
		aim.addAIMListener(this);
		new Thread(aim).start();
	}
	private void sendIntent(String purpose, Bundle b){
		b.putString("username",username);
		b.putString("password",password);
		b.putString("className", getClass().getName().toString());
		b.putInt("connectionId", connectionId);
		
		Intent toAM = new Intent();
		toAM.setAction("AndroidMessenger"); 
		toAM.putExtra("purpose", purpose);
		toAM.putExtras(b);	
		broadcastIntent(toAM);
	}


	/* Aim Listener Event Handlers */
	public void handleConfigReady(Map<String,AIMBuddy> config){
		Bundle b = new Bundle();
		for(String s : config.keySet()){
			AIMBuddy buddy = config.get(s);
			Bundle bud = new Bundle();
			bud.putString("name",buddy.getName());
			bud.putString("group", buddy.getGroup());
			bud.putString("alias", buddy.getAlias());
			b.putBundle(s, bud);
		}
		sendIntent("config",b);
	}
	
	public void handleBuddySignOff(AIMBuddy buddy, String info) {
		Bundle b = new Bundle();
		b.putString("name",buddy.getName());
		b.putString("alias",buddy.getAlias());
		b.putString("group", buddy.getGroup());
		b.putString("protocol",AMProtocolMapper.getName(this.getClass()));
		sendIntent("buddySignOff", b);
	}

	public void handleBuddySignOn(AIMBuddy buddy, String info) {
		Bundle b = new Bundle();
		b.putString("name",buddy.getName());
		b.putString("alias",buddy.getAlias());
		b.putString("group", buddy.getGroup());
		b.putString("protocol",AMProtocolMapper.getName(this.getClass()));
		sendIntent("buddySignOn", b);
	}
	public void handleConnected() {
		Bundle b = new Bundle();
		sendIntent("signOnSuccess",b);
		connected = true;
	}
	
	public void handleDisconnected() {
		if(connected){
			connected = false;
			Bundle b = new Bundle();
			sendIntent("disconnected",b);
		}
	}

	public void handleError(String error, String message) {
		Bundle b = new Bundle();
		b.putString("error", message);
		sendIntent("signOnError",b);
	}

	public void handleMessage(AIMBuddy buddy, String request) {
		Bundle b = new Bundle();
		b.putString("name",buddy.getName());
		b.putString("alias",buddy.getAlias());
		b.putString("message", request);
		sendIntent("messageRecieved", b);

	}

	public void handleWarning(AIMBuddy buddy, int amount) {
		/* Unimplemented */
	}
	public void handleBuddyAvailable(AIMBuddy buddy, String message) {		
		/* UnImplemented
		Bundle b = new Bundle();
		b.putString("name",buddy.getName());
		b.putString("group", buddy.getGroup());
		b.putString("protocol",AMProtocolMapper.getName(this.getClass()));
		sendIntent("buddyBack", b);
		*/
	}

	public void handleBuddyUnavailable(AIMBuddy buddy, String message) {
		/* UnImplemented in Messenger
		Bundle b = new Bundle();
		b.putString("name",buddy.getName());
		b.putString("group", buddy.getGroup());
		b.putString("protocol",AMProtocolMapper.getName(this.getClass()));
		sendIntent("buddyAway", b);
		*/
	}





}
