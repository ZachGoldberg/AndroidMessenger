/**
 * @author - Zachary Goldberg @ 2008
 */

package com.penn.cis121.androidmessenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;

public class ConversationContainer extends Activity {
	private List<Button> buttons;
	private List<String> windows;
	private HashMap<String, String> editing; // Buddy -> currently editing
	// text
	private HashMap<String, List<String>> messages; // buddy -> message
	// conversation
	private HashMap<String, String> nameFromAlias;

	/* Store information on the connection for each buddy */
	private Map<String, String> buddyToMe;
	private Map<String, Integer> buddyToConnectionId;

	private String currentBuddy;
	private ButtonListener bl;
	private String lastMessage = "";
	private Logger logger;
	private LinearLayout currentRow;
	private LinearLayout rowContainer;
	private EditText input;

	private ScrollView scroller;
	private Button closeButton;

	private ConversationReciever aml;


	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		logger = Logger.getLogger(this.getClass().getName());
		
		setupUIComponents();
		
		aml = new ConversationReciever();
		registerReceiver(aml, new IntentFilter("AndroidMessenger"));

		windows = new ArrayList<String>();
		editing = new HashMap<String, String>();
		messages = new HashMap<String, List<String>>();
		nameFromAlias = new HashMap<String, String>();
		buttons = new ArrayList<Button>();
		buddyToMe = new HashMap<String, String>();
		buddyToConnectionId = new HashMap<String, Integer>();
		
		makeWindows();
		reloadPrefs();
		
		Intent i = getIntent();
		Bundle bb = i.getExtras();
		String alias = bb.getString("alias");
		String name = bb.getString("name");
		String me =  i.getStringExtra("username");
		
		if (alias != null && name != null) {
			if(!windows.contains(name)){
				buddyToMe.put(name, me);
				buddyToConnectionId.put(me,bb.getInt("connectionId"));
				nameFromAlias.put(alias, name);
				messages.put(name, new LinkedList<String>());
				editing.put(name, "");
				Button b = addButton(name);
				b.performClick(); //fire the onFocusChanged				
			}else{
				for(Button b : buttons){
					if(b.getText().toString().equals(name)){
						b.performClick();
					}
				}
			}
			currentBuddy = name;
		}else{
			finish(); //Something not good happened
		}	
				
		/*
		 * Handle any messages received prior to opening the conversation
		 * panel.
		 */
		for (int x = 0; x < bb.getInt("numwaiting"); x++) {
			Bundle b = bb.getBundle(x + "");
			handleMessage(b.getString("name"), b.getString("message"), b
					.getString("username"), b.getInt("connectionId"));
			logger.severe(b.getString("name") + "," + b.getString("message"));
		}
		
		regenLayout();
	}
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(aml);
	}
	
	
	private void setupUIComponents(){
		setContentView(R.layout.conversationcontainer);
		bl = new ButtonListener();
		rowContainer = (LinearLayout) findViewById(R.id.buttonRows);
		closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(bl);
		scroller = (ScrollView) findViewById(R.id.scrollBox);
		input = (EditText) findViewById(R.id.inputBox);
		input.setOnKeyListener(bl);
	}

	private Button addButton(String text) {
		logger.severe("Adding button for " + text);
		Button nb = new Button(this);
		nb.setText(text);
		setProps(nb);
		if (currentRow == null || currentRow.getChildCount() == 3) {
			LinearLayout newrow = new LinearLayout(this);
			newrow.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			newrow.setOrientation(LinearLayout.HORIZONTAL);
			rowContainer.addView(newrow);
			currentRow = newrow;
		}
		currentRow.addView(nb);
		rowContainer.requestLayout();
		windows.add(text);
		return nb;
	}

	private void removeButton(String name) {
		if (currentRow == null)
			return;
		// Easiest way to do this is just going to be to entirely redo the buttons
		if (!windows.remove(name)) {
			logger.severe("Problem removing " + name);
		}
		buttons.clear();
		currentRow = null;
		rowContainer.removeAllViews();
		List<String> oldWindows = new ArrayList<String>();
		for (String s : windows)
			oldWindows.add(s);
		windows.clear();
		for (String s : oldWindows) {
			addButton(s);
		}
	}

	private void setProps(Button b) {
		b.setOnClickListener(bl);
		b.setOnFocusChangeListener(bl);
		if (windows.size() >= 3 && b.getText().toString().length() > 10) {
			b.setText(b.getText().toString().substring(0, 10) + "..");
		}
		b.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		buttons.add(b);
	}

	private void regenLayout() {
		TextView messageBox = (TextView) findViewById(R.id.messageBox);
		messageBox.setText("");
		if (messages.get(currentBuddy) == null) {
			logger.warning(currentBuddy + " has no message list?");
			return;
		}
		String newText="";
		for (String s : messages.get(currentBuddy)) {
			newText += s;			
		}
		messageBox.setText(newText);		
		EditText editBox = (EditText) findViewById(R.id.inputBox);
		editBox.setText(editing.get(currentBuddy));		
		/*
		 * I could not get this to ever scroll all the way to the bottom.  The problem is that the UI 
		 * has not yet updated to the new text.  So it scrolls to the bottom of the text pre-new text.
		 * I've spent hours trying to find a hack.  I encourage you to try and best me.  Things i've tried:
		 * 
		 * Playing with requestLayout,computeScroll(),invalidate() and notify() on both the messageBox and scroller objects
		 * Setting up a TextWatcher on the messageBox to reset the scroll onTextChanged
		 * Setting up a new thread and setting a timer in that thread.  When the timer expires try and update the window (Give the UI time to update).
		 *          Does not work because you can't update the UI from another thread
		 *          So I tried to set an uncaughtExceptionHandler on the thread and then in the thread divide by zero to throw an exception.
		 *          Then 'this' thread would have the uncaughtExceptionHandler and be able to update the UI.
		 *          This caused all sorts of BAD behavior in many many many ways.  Don't try it.
		 *          
		 * Labeled as an Android Deficiency for now.
		 */
		messageBox.requestLayout();
		messageBox.invalidate();
		scroller.fullScroll(ScrollView.FOCUS_DOWN);		
	}

	private String currentInput() {
		EditText editBox = (EditText) findViewById(R.id.inputBox);
		return editBox.getText().toString();
	}

	private void handleMessage(String buddyName, String message,
			String username, int connectionId) {
		
		if (!buddyToMe.containsKey(buddyName))
			buddyToMe.put(buddyName, username);
		if(!buddyToConnectionId.containsKey(username)){
			buddyToConnectionId.put(username,connectionId);
		}		
		if (!messages.containsKey(buddyName))
			messages.put(buddyName, new LinkedList<String>());
		
		lastMessage = buddyName + ": " + message + "\n";
		messages.get(buddyName).add(lastMessage);
		boolean found = false;
		logger.warning(buddyName + "," + currentBuddy + ","	+ (buddyName.equals(currentBuddy)));
		if (!buddyName.equals(currentBuddy)) {
			for (Button b : buttons)
				// Find the right button and add a *
				if (b.getText().equals(buddyName)) {
					b.setText("*" + buddyName);
					found = true;
				}
			if (!found)
				addButton("*" + buddyName);
		}
		regenLayout();
	}

	private class ButtonListener implements OnFocusChangeListener,
			OnClickListener, OnKeyListener {

		public void onFocusChanged(View v, boolean hasFocus) {
			// Set all buttons to not focusable
			if (hasFocus == true) {
				for (Button b : buttons) {
					if (!b.getText().equals(((Button) v).getText()))
						b.clearFocus();
				}
			}
			// Save the current input
			editing.put(currentBuddy, currentInput());
			Button b = (Button) v;
			if (b.getText().charAt(0) == '*') {
				b.setText(b.getText().toString().substring(1)); // get rid of the *
			}
			currentBuddy = nameFromAlias.get(b.getText());
			if (currentBuddy == null)
				currentBuddy = b.getText().toString();
			TextView tv = (TextView) findViewById(R.id.status);
			tv.setText("Conversation with " + currentBuddy);
			regenLayout();
		}
		public void onClick(View button) {
			if (button != closeButton){
				onFocusChanged(button, true);
			}else {
				if (windows.size() <= 1) { 
					//Close the whole panel if we're closing the last buddy
					windows.remove(currentBuddy);	
					finish();
					return;
				}
				removeButton(currentBuddy);
				currentBuddy = windows.get(0);
				for (Button bv : buttons) {
					if (bv.getText().equals(currentBuddy))
						bv.requestFocus();
				}
			}
		}
		public boolean onKey(View view, int keyCode, KeyEvent event) {
			//We only want key events for the input window
			if (view != input)
				return false;
			//For debugging
			if (keyCode == KeyEvent.KEYCODE_MINUS){
				scroller.fullScroll(ScrollView.FOCUS_DOWN);
			}
			//If we actually want to send a message
			if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
					|| keyCode == KeyEvent.KEYCODE_NEWLINE) {
				String from = buddyToMe.get(currentBuddy);
				String msg = input.getText().toString();
				if (msg.length() == 0)
					return false;
				if((from + ": " +msg + "\n").equals(lastMessage))
					return false;
				Intent i = new Intent();
				i.setAction("AndroidMessenger");
				i.putExtra("purpose", "sendMessage");
				i.putExtra("connectionId",buddyToConnectionId.get(from));
				i.putExtra("message", msg);
				i.putExtra("buddyName", currentBuddy);
				broadcastIntent(i);
				input.setText("");
				lastMessage = from + ": " + msg + "\n";
				messages.get(currentBuddy).add(lastMessage);
				regenLayout();
				return true;
			}
			return false;
		}
	}
	private class ConversationReciever extends IntentReceiver {
		@Override
		public void onReceiveIntent(Context arg0, Intent intent) {
			String purpose = intent.getStringExtra("purpose");
			if (purpose.equals("messageRecieved")) {
				handleMessage( intent.getStringExtra("name"),
						intent.getStringExtra("message"),
						intent.getStringExtra("username"), 
						intent.getIntExtra("connectionId",0)
						);
			}
		}

	}
	
	/*
	 * The below functions can be classified as 
	 * "Terrible horrible ugly bad never do this its no good HACK" 
	 * (Only the first 3 are used.  The second 2 are for if we could onFreeze() but we can't)
	 * (The actual hack is using SharedPreferences to emulate persistent memory)
	 * 
	 * 
	 *  I've had some discussion with Android developers on IRC and read
	 *  lots of documentation; and thus far there is no _good_ way of maintaining state
	 *  in between switches back and fourth from conversation page to the 
	 *  main buddy list.  Hence every time we re-open the conversation pain we need to
	 *  rebuild all of the UI from saved (serialized) state.  The below code does that 
	 *  saving and serialization.  Even the way we're saving data isn't the best way
	 *  and likely leaks a bit.  Its just _bad_.  But as the SDK develops i'm sure
	 *  a better solution will arise.  For now we leave it.
	 * 
	 */
	private void makeWindows(){
		SharedPreferences prefs = getSharedPreferences(getClass().toString()+"windows", 0);
		 Map<String,? extends Object> map = prefs.getAll();
		 for(String s : map.keySet()){
			 logger.severe("Adding button for " + s);
			 addButton(s);
		 } 		 
	}
	public void reloadPrefs() {			
		 SharedPreferences prefs = getSharedPreferences(getClass().toString()+"editing", 0);
		 Map<String,? extends Object> map = prefs.getAll();
         for(String buddy : map.keySet()){
        	 editing.put(buddy, (String)map.get(buddy));                       
         }
         prefs = getSharedPreferences(getClass().toString()+"aliases", 0);
         map = prefs.getAll();
         for(String buddy : map.keySet()){
        	 nameFromAlias.put(buddy, (String)map.get(buddy));                       
         }
         prefs = getSharedPreferences(getClass().toString()+"tome", 0);
         map = prefs.getAll();
         for(String buddy : map.keySet()){
        	 buddyToMe.put(buddy, (String)map.get(buddy));                       
         }
         prefs = getSharedPreferences(getClass().toString()+"meid", 0);
         map = prefs.getAll();
         for(String buddy : map.keySet()){
        	 buddyToConnectionId.put(buddy, (Integer)map.get(buddy));                       
         }
         for(String buddy : windows){
             prefs = getSharedPreferences(getClass().toString()+"messages"+buddy, 0);
             map = prefs.getAll();
             if(!messages.containsKey(buddy))
            	 messages.put(buddy,new ArrayList<String>());
             logger.info("Restoring messages for " + buddy + " (#" + map.keySet().size()+")");
             for(String message : map.keySet()){
            	 messages.get(buddy).add(message);                       
             }
         }
	}
	
    @Override
    protected void onPause() {
        super.onPause();        
        SharedPreferences.Editor editor = getSharedPreferences(getClass().toString()+"windows", 0).edit();        
        for(String s : windows){
        	editor.putString(s,s);
        }
        editor.commit();
        editor = getSharedPreferences(getClass().toString()+"aliases", 0).edit();
        for(String buddy : nameFromAlias.keySet()){                      
            editor.putString(buddy,nameFromAlias.get(buddy));                       
        }
        editor.commit();
        editor = getSharedPreferences(getClass().toString()+"tome", 0).edit();
        for(String buddy : buddyToMe.keySet()){                      
            editor.putString(buddy,buddyToMe.get(buddy));                       
        }
        editor.commit();
        editor = getSharedPreferences(getClass().toString()+"meid", 0).edit();
        for(String buddy : buddyToConnectionId.keySet()){                      
            editor.putInt(buddy,buddyToConnectionId.get(buddy));                       
        }
        editor.commit();
        editor = getSharedPreferences(getClass().toString()+"editing", 0).edit();
        for(String buddy : editing.keySet()){                      
            editor.putString(buddy,editing.get(buddy));                       
        }
        editor.commit();
        for(String buddy : messages.keySet()){
            editor = getSharedPreferences(getClass().toString()+"messages"+buddy, 0).edit();
            logger.info("Saving messages for " + buddy + " (#" + messages.get(buddy).size()+")");
            for(String msg : messages.get(buddy))
            	editor.putString(msg,"");
            editor.commit();            
        }        
    }
    /*	
	private void restoreFromBundle(Bundle state) {
		rowContainer.removeAllViews();
		
		Bundle windowBundle = state.getBundle("windowBundle");
		Bundle NFABundle = state.getBundle("NFABundle");
		Bundle BTMBundle = state.getBundle("BTMBundle");
		Bundle MPWBundle = state.getBundle("MPWBundle");
		Bundle MClassBundle = state.getBundle("MClassBundle");
		Bundle EditingBundle = state.getBundle("EditingBundle");
		Bundle MessageBundle = state.getBundle("MessageBundle");
		
		for (String s : windowBundle.keySet())
			addButton(s);
		
		for (String buddy : EditingBundle.keySet())
			editing.put(buddy, (String) EditingBundle.getString(buddy));
		
		for (String buddy : NFABundle.keySet())
			nameFromAlias.put(buddy, (String) NFABundle.getString(buddy));
		
		for (String buddy : BTMBundle.keySet())
			buddyToMe.put(buddy, (String) BTMBundle.getString(buddy));
		
		for (String buddy : MPWBundle.keySet())
			mePw.put(buddy, (String) MPWBundle.getString(buddy));
		
		for (String buddy : MClassBundle.keySet())
			meClass.put(buddy, (String) MClassBundle.getString(buddy));
		
		for (String buddy : MessageBundle.keySet()) {
			if (!messages.containsKey(buddy))
				messages.put(buddy, new ArrayList<String>());
			messages.get(buddy).add(MessageBundle.getString(buddy));
		}
	}

	/**
	 * Any time we are paused we need to save away the current state, so it will
	 * be restored correctly when we are resumed.
	 *//*
	@Override
	protected void onFreeze(Bundle outBundle) {
		logger.severe("Inside onFreeze");
		Bundle windowBundle = new Bundle();
		Bundle NFABundle = new Bundle();
		Bundle BTMBundle = new Bundle();
		Bundle MPWBundle = new Bundle();
		Bundle MClassBundle = new Bundle();
		Bundle EditingBundle = new Bundle();
		Bundle MessageBundle = new Bundle();
		for (String s : windows)
			windowBundle.putString(s, s);
		for (String buddy : nameFromAlias.keySet())
			NFABundle.putString(buddy, nameFromAlias.get(buddy));
		for (String buddy : buddyToMe.keySet())
			BTMBundle.putString(buddy, buddyToMe.get(buddy));
		for (String buddy : mePw.keySet())
			MPWBundle.putString(buddy, mePw.get(buddy));
		for (String buddy : meClass.keySet())
			MClassBundle.putString(buddy, meClass.get(buddy));
		for (String buddy : editing.keySet())
			EditingBundle.putString(buddy, editing.get(buddy));
		for (String buddy : messages.keySet()) {
			String big = "";
			for (String msg : messages.get(buddy))
				big += msg;
			MessageBundle.putString(buddy, big);
		}
		outBundle.putBundle("windowBundle", windowBundle);
		outBundle.putBundle("NFABundle", NFABundle);
		outBundle.putBundle("BTMBundle", BTMBundle);
		outBundle.putBundle("MPWBundle", MPWBundle);
		outBundle.putBundle("MClassBundle", MClassBundle);
		outBundle.putBundle("EditingBundle", EditingBundle);
		outBundle.putBundle("MessageBundle", MessageBundle);
		super.onFreeze(outBundle);
	}*/
}
