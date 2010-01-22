/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.text.Layout;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu.Item;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.penn.cis121.androidmessenger.accountprovider.AccountInfo;

public class AndroidMessenger extends ListActivity {

	/** Constants **/
	private final int ADD_OPTION = Menu.FIRST;
	
	private final int EDIT_OPTION = ADD_OPTION + 1;
	private final int EXIT_OPTION = EDIT_OPTION + 1;
	private final int EDIT_ACCOUNT = 0;
	
	/** Data Storage **/
	private List<ServiceConnection> serviceConnections;
	private List<ConnectionView> connectionViews;
	private HashMap<Integer,ConnectionView> viewsById;
	
	/** Instance Vars **/
	private AMListener aml;
	private BuddyListAdapter bla;
	private Logger logger;
	private boolean connectionWindowOpen = false;
	private List<Bundle> waiting;
	private int nextViewId = 1;
	/*
	 * Android LifeCycle Functions
	 */

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	
		
		// Necessary data structures
		logger = Logger.getLogger(AndroidMessenger.class.getName());
		serviceConnections = new ArrayList<ServiceConnection>();
		connectionViews = new ArrayList<ConnectionView>();
		waiting = new LinkedList<Bundle>();
		viewsById = new HashMap<Integer, ConnectionView>();
		
		// Setup the intent filter that will listen to services
		aml = new AMListener(this);
		registerReceiver(aml, new IntentFilter("AndroidMessenger"));
		
		//Clear junk left over from the hack ConversationContinaer
		clearPrefs();
		
		try {
			/* VOODOO!
			 * 
			 * Instantiate each class once to force java
			 * to run all of the static{} blocks
			 * which register with AMProtocolMapper.
			 * 
			 * statc {} blocks are run when a class is loaded by the java 
			 * runtime environment.  We need those blocks to be run to know 
			 * certain information about that class.  So we force them to
			 * be loaded by finding them all, and making a fake instance 
			 * of them.  This enables you to simply write your own 
			 * protocol files, write an appropriate static {} block,
			 * throw it in the proper package, and it'll just work.	
			 * 
			 * Basically consider it some reflection magic.
			 */			
			PackageManager m = getPackageManager();
			String pn = getPackageName();
			PackageInfo pi = m.getPackageInfo(pn, PackageManager.GET_SERVICES);
			for (android.content.pm.ServiceInfo s : pi.services) {
				Class<? extends Object> c = getClassLoader().loadClass(s.name);
				@SuppressWarnings("unused")
				Object o = c.newInstance();		
				
			}
		} catch (Exception e) {
			/* So many things cold possible go wrong with reflection
			 * if any of them do we don't care too much so do nothing
			 */
		}

		// Miscellaneous
		setDefaultKeyMode(SHORTCUT_DEFAULT_KEYS);
		
		// Setup the buddy list adapter
		bla = new BuddyListAdapter(this);
		
		//Load info from the database
		loadConnections();
				
		setListAdapter(bla);	
	}
	
	/** Called when we come back from conversationcontainer or anything else */
	@Override
	protected void onResume() {
		super.onResume();
		connectionWindowOpen = false;
		bla.clearAllMessageRecieved();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Stop and unbind all the services
		for (ServiceConnection cv : serviceConnections) {
			unbindService(cv);
			stopService(new Intent(this, cv.getClass()));
		}
	}

	/* Logic Functions */
	
	/**
	 * A function which interacts with our AccountProvider class to load 
	 * all of the currently saved connections.  Populates instance fields.
	 */
	private void loadConnections() {
		//Some voodoo to use the database to load saved connection information.
		logger.severe("Loading Connections");
		Uri curi = AccountInfo.Account.CONTENT_URI;
		Cursor cur = managedQuery(curi, null, null, null);
		List<ConnectionView> TempConnectionList = new ArrayList<ConnectionView>();
		if (cur.first()) {
			String username, password, className;
			int nameColumn = cur.getColumnIndex(AccountInfo.Account.USERNAME);
			int passwordColumn = cur.getColumnIndex(AccountInfo.Account.PASSWORD);
			int classColumn = cur.getColumnIndex(AccountInfo.Account.CLASSNAME);
			do {
				// Get the field values
				username = cur.getString(nameColumn);
				password = cur.getString(passwordColumn);
				className = cur.getString(classColumn);
				logger.severe("Found connection for " + username);
				// check for duplicates
				ConnectionView found = getConnection(username, password,className);
				if (found == null) { // don't want to re-make the object. We
								  	 // would lost the connected status etc.
					ConnectionView aimview;
					try {
						Class<? extends Object> c = getClassFromString(className);
						int id = nextViewId++;
						aimview = new ConnectionView(id,this, username, password, c);
						aimview.setText(AMProtocolMapper.getName(c) + ": " + username);
						aimview.setPadding(2, 2, 2, 2);
						aimview.setLayoutParams(new LinearLayout.LayoutParams(
								LayoutParams.FILL_PARENT,
								LayoutParams.FILL_PARENT));
						TempConnectionList.add(aimview);
					} catch (ClassNotFoundException e) {
						// delete it from the DB
						getContentResolver().delete(
								AccountInfo.Account.CONTENT_URI,
								AccountInfo.Account.USERNAME + " = \""
										+ username + "\" AND "
										+ AccountInfo.Account.PASSWORD
										+ " = \"" + password + "\" AND "
										+ AccountInfo.Account.CLASSNAME
										+ " = \"" + className + "\"", null);
					}
				} else {
					TempConnectionList.add(found);
				}
			} while (cur.next());
		}
		//Now reset and read all proper connections
		connectionViews.clear();
		bla.clearConnections();		
		for (ConnectionView cv : TempConnectionList) {
			connectionViews.add(cv);
			bla.addConnection(cv);
			viewsById.put(cv.getId(), cv);
		}
		bla.regenList();
	}
	/**
	 * A function to get a class from its name
	 * 
	 * @param classname The class name to look up
	 * @return the class represented by <i>classname</i> retrieved from the classLoader
	 * @throws ClassNotFoundException
	 */
	private Class<? extends Object> getClassFromString(String classname)
			throws ClassNotFoundException {
		return getClass().getClassLoader().loadClass(classname);
	}
	/**
	 * Parse and handle a config coming from the AIM thread
	 * 
	 * @param config - a bunch of options we need to work with
	 * @param cv - the connection the config came from
	 */
	private void parseConfig(Bundle config, ConnectionView cv) {
		for (String s : config.keySet()) {
			try {
				Bundle buddy = config.getBundle(s);
				if (buddy == null)
					continue;
				BuddyView bv = new BuddyView(this, buddy.getString("name"),
						buddy.getString("group"), buddy.getString("alias"), cv);
				bla.addBuddy(bv);
			} catch (ClassCastException c) {
				// Some of these will happen, (username etc.) its OK
			}
		}
		bla.regenList();
	}
	/**
	 * remove all buddys in the cv from the buddylist
	 * @param cv the connection whose buddies need to be removed
	 */
	private void removeBuddies(ConnectionView cv) {
		// TODO write this
		/*
		 * if(cv == null) return; for(BuddyView bv :
		 * buddiesOnConnection.get(cv)){ bla.removeBuddy(bv); } bla.regenList();
		 */
	}
	/**
	 * Generate a new  and add it to our internal list of connecitons
	 * 
	 * @param username
	 * @param password
	 * @param connectionClass
	 * @return the new ServiceConnection
	 */
	private ServiceConnection nextServiceConnection(int id,String username,	String password) {
		ServiceConnection m = new AMServiceConnection(id,username, password);
		serviceConnections.add(m);
		return m;
	}
	/**
	 * Lookup a ConnectionView by Id
	 * 
	 * @param id
	 * @return 
	 */
	private ConnectionView getConnection(int id) {
		return viewsById.get(id);
	}
	/**
	 * Lookup a ConnectionView by username+pw+className
	 * 
	 * @param username
	 * @param password
	 * @param className
	 * @return the connectionview if found
	 */
	private ConnectionView getConnection(String username, String password, String className) {
		for (ConnectionView cv : connectionViews) {
			if (cv.getUsername().equals(username)
					&& cv.getPassword().equals(password)
					&& cv.getConnectionClass().getName().toString().equals(
							className)) {
				return cv;
			}
		}
		logger.warning("Could not find connection for " + username + "," + password + "," + className);
		return null;
	}

	/*
	 * EventHandlers and CallBacks
	 */
	/**
	 * When an item is clicked in the buddy list
	 */	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (v instanceof ConnectionView) {
			ConnectionView cv = (ConnectionView) v;
			ServiceConnection sc = nextServiceConnection(cv.getId(),cv.getUsername(), cv.getPassword());
			bindService(new Intent(this, cv.getConnectionClass()), sc,BIND_AUTO_CREATE);
			// TODO set some sort of global status if paused?
		} else if (v instanceof BuddyView) {
			Intent i = new Intent(this, ConversationContainer.class);
			Bundle bigBundle = new Bundle();
			bigBundle.putString("name", ((BuddyView) v).getName());
			bigBundle.putString("alias", ((BuddyView) v).getAlias());
			bigBundle.putString("username", ((BuddyView) v).getConnection().getUsername());
			bigBundle.putInt("connectionId", ((BuddyView) v).getConnection().getId());
			int ic = 0;
			for (Bundle b : waiting) {
				bigBundle.putBundle(ic + "", b);
				ic++;
			}
			waiting.clear();
			bigBundle.putInt("numwaiting", ic);
			i.putExtras(bigBundle);
			connectionWindowOpen = true;
			startActivity(i);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ADD_OPTION, R.string.add_account);
		menu.add(0, EDIT_OPTION, R.string.edit_account);
		menu.add(0, EXIT_OPTION, R.string.exit_text);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, Item item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getId()) {
		case ADD_OPTION:
			Intent i = new Intent();
			i.setClass(this, AccountEditor.class);
			startSubActivity(i, EDIT_ACCOUNT);
			break;
		case EDIT_OPTION:
			View cv = getListView().getSelectedView();
			if (cv instanceof ConnectionView) {
				Intent ii = new Intent();
				ConnectionView cvs = (ConnectionView) cv;
				ii.putExtra("username", cvs.getUsername());
				ii.putExtra("password", cvs.getPassword());
				ii.putExtra("className", cvs.getConnectionClass().getName()
						.toString());
				ii.setClass(this, AccountEditor.class);
				startSubActivity(ii, EDIT_ACCOUNT);
			}
			break;
		case EXIT_OPTION:
			finish();
			break;

		}
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode,
			String data, Bundle extras) {
		switch (requestCode) {
		case EDIT_ACCOUNT:
			loadConnections();
		}

	}

	/* Private classes */
	
	/**
	 * Buddy List Adapter -- our version of a BaseAdapter which provides
	 * buddy list like functionality.  Incorporates necessary elements
	 * for organizing lists as follows:
	 * <CONNECTIONS>
	 * <GROUP G>
	 *   <BUDDIES In G>
	 *   
	 */
	private class BuddyListAdapter extends BaseAdapter {
		int count = 0;
		private Context c;
		private List<ConnectionView> cvs;
		private HashMap<String, GroupView> groups;
		private HashMap<String, HashMap<String, BuddyView>> buddyList; // Group
		// ->
		// list<BuddyView>
		private List<View> renderedViews;

		public BuddyListAdapter(Context c) {
			this.c = c;
			cvs = new ArrayList<ConnectionView>();
			buddyList = new HashMap<String, HashMap<String, BuddyView>>();
			groups = new HashMap<String, GroupView>();
		}

		public int getCount() {
			return count;
		}

		public Object getItem(int position) {
			return renderedViews.get(position);
		}

		public long getItemId(int position) {
			return ((View) getItem(position)).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			return renderedViews.get(position);
		}

		public void addConnection(ConnectionView cv) {
			cvs.add(cv);
			regenList();
		}

		public void addBuddy(BuddyView bv) {
			String group = bv.getGroup();
			if (!buddyList.containsKey(group))
				buddyList.put(group, new HashMap<String, BuddyView>());
			buddyList.get(group).put(bv.getAlias().toLowerCase(), bv);
			if (!groups.containsKey(group))
				groups.put(group, new GroupView(c, group));
		}

		public void removeBuddy(BuddyView bv) {
			buddyList.get(bv.getGroup()).remove(bv.getAlias().toLowerCase());
		}

		public void setBuddyOnline(String alias, String group) {
			if (alias == null)
				return;
			if (!buddyList.containsKey(group)) {
				logger.severe("Unknown group:" + group);
				return;
			}
			if (!buddyList.get(group).containsKey(alias.toLowerCase())) {
				logger.severe("Unknown buddy:" + alias);
				return;
			}
			logger.info("Buddy SignOn: " + alias + " group" + group);
			buddyList.get(group).get(alias.toLowerCase()).setOnline(true);
			regenList();
		}

		public void setBuddyOffline(String alias, String group) {
			buddyList.get(group).get(alias.toLowerCase()).setOnline(false);
			regenList();
		}

		public void setBuddyMessageRecieved(String name) {
			for (String group : buddyList.keySet()) {
				if (buddyList.get(group).containsKey(name)) {
					BuddyView v = buddyList.get(group).get(name);
					v.setMessageRecieved(true);
				}
			}
			regenList();
		}

		public void clearAllMessageRecieved() {
			for (String group : buddyList.keySet()) {
				for (View v : buddyList.get(group).values()) {
					if (v instanceof BuddyView) {
						BuddyView bv = (BuddyView) v;
						bv.setMessageRecieved(false);
					}
				}
			}
			regenList();
		}

		public void clearConnections() {
			cvs.clear();
		}

		public void regenList() {
			renderedViews = new LinkedList<View>();
			for (ConnectionView cv : cvs) {
				if (!cv.getConnected())
					renderedViews.add(cv);
			}
			List<String> ls = new ArrayList<String>(groups.keySet());
			Collections.sort(ls);
			for (String group : ls) {
				HashMap<String, BuddyView> map = buddyList.get(group);
				List<String> buddies = new ArrayList<String>(map.keySet());
				if (buddies == null)
					continue;
				boolean groupshown = false;
				Collections.sort(buddies);
				for (String buddyAlias : buddies) {
					BuddyView bv = map.get(buddyAlias);
					if (bv.isOnline()) {
						if (!groupshown) {
							renderedViews.add(groups.get(group));
							groupshown = true;
						}
						renderedViews.add(bv);
					}
				}
			}
			count = renderedViews.size();
			// notifyDataSetChanged();// may need this one too, not sure yet
			// check for empty groups and remove
			notifyDataSetInvalidated();
			notifyDataSetChanged();
		}
	}

	private class AMServiceConnection implements ServiceConnection {

		private String username;
		private String password;
		private int cvId;

		public AMServiceConnection(int id,String username, String password) {
			this.cvId = id;
			this.username = username;
			this.password = password;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			ConnectionView cv = getConnection(cvId);
			AMService am = AMService.Stub.asInterface((IBinder) service);
			cv.setService(am);
			try {
				am.Connect(username, password,cvId);
				cv.setText("Connecting to "
						+ AMProtocolMapper.getName(cv.getConnectionClass())
						+ "...");
			} catch (DeadObjectException e) {
				// TODO tell the AndroidMessenger that this service died and
				// should be remade or something
			}
		}

		public void onServiceDisconnected(ComponentName name) {
			ConnectionView cv = getConnection(cvId);
			cv.setService(null);
			// TODO tell the AndroidMessenger that this service died and should
			// be remade or something
		}

	}

	private class GroupView extends TextView {
		private String group;

		public GroupView(Context context, String group) {
			super(context);
			this.group = group;
			setText(group);
			setAlignment(Layout.Alignment.ALIGN_CENTER);
			setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			setFocusable(false);
			setPadding(2, 2, 2, 2);
		}

		public String getGroup() {
			return group;
		}
	}

	private class BuddyView extends TextView {
		private String name;
		private String group;
		private String alias;
		private ConnectionView connection;
		private boolean online;
		private boolean received = false;

		public BuddyView(Context context, String name, String group,
				String alias, ConnectionView cv) {
			super(context);
			this.name = name;
			this.group = group;
			this.alias = alias;
			this.connection = cv;
			if (alias == null)
				this.alias = name;
			if (this.alias != null && this.alias.length() != 0)
				setText(this.alias);
			else
				setText(this.name);
			setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			setFocusable(false);
			setPadding(2, 2, 2, 2);
			// status?
			// Image for Icon?
		}

		public void setMessageRecieved(boolean receivedMsg) {
			if (receivedMsg && !received)
				setText("*" + this.alias);
			else if (!receivedMsg && received)
				setText(this.alias);
			this.received = receivedMsg;
		}

		public ConnectionView getConnection() {
			return connection;
		}

		public String getName() {
			return name;
		}

		public String getGroup() {
			return group;
		}

		public String getAlias() {
			return alias;
		}

		public void setOnline(boolean b) {
			online = b;
		}

		public boolean isOnline() {
			return online;
		}

	}

	private class ConnectionView extends TextView {
		private boolean connected = false;;
		private String username;
		private String password;
		private int id;
		private Class<? extends Object> connectionClass;
		private AMService am;

		public ConnectionView(int id, Context context, String username,
				String password, Class<? extends Object> connectionClass) {
			super(context);
			this.id = id;
			this.username = username;
			this.password = password;
			this.connectionClass = connectionClass;
		}

		public void setConnected(boolean c) {
			connected = c;
		}
		public int getId(){
			return id;
		}
		public boolean getConnected() {
			return connected;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public Class<? extends Object> getConnectionClass() {
			return connectionClass;
		}

		public void setService(AMService am) {
			this.am = am;
		}

		public AMService getService() {
			return am;
		}
	}

	private class AMListener extends IntentReceiver {
		private AndroidMessenger c;

		public AMListener(AndroidMessenger c) {
			this.c = c;
		}

		@Override
		public void onReceiveIntent(Context context, Intent intent) {

			String purpose = (String) intent.getStringExtra("purpose");
			logger.info("Recieved intent with purpose: " + purpose);
			ConnectionView cv = null;
			int id = intent.getIntExtra("connectionId",-1);
			if(id != -1){
				cv = getConnection(id);
			}else{
				cv = getConnection( intent
						.getStringExtra("username"), intent
						.getStringExtra("password"), intent
						.getStringExtra("className"));
			}
			if (cv == null) {
				logger.severe("COULDNT FIND CV.... BAD!!!!!!!!!!!!!!!!!");
				logger.severe("Tried: " + id + ","+ intent.getStringExtra("username"));
				return;
			}
			if (purpose.equals("signOnSuccess")) {
				cv.setText("Connected!");
				cv.setConnected(true);
				bla.notifyDataSetChanged();
			} else if (purpose.equals("config")) {
				parseConfig(intent.getExtras(), cv);
			} else if (purpose.equals("resumeBuddyList")) {
				c.onResume();
				c.getWindow().makeActive();
				c.getTaskId();
				c.getWindow().closeAllPanels();
				getWindow().makeActive();

			} else if (purpose.equals("sendMessage")) {
				try {
					AMService am = cv.getService();
					if (am != null)
						am.sendMessage(intent.getStringExtra("buddyName"),
								intent.getStringExtra("message"));
					else {
						// TODO error
					}
				} catch (DeadObjectException e) {
					// TODO Close the service
				}
			} else if (purpose.equals("signOnError")) {
				cv.setText("Error: " + intent.getStringExtra("error"));
				cv.setConnected(false);
			} else if (purpose.equals("buddySignOn")) {
				bla.setBuddyOnline(intent.getStringExtra("alias"), intent
						.getStringExtra("group"));
			} else if (purpose.equals("buddySignOff")) {
				bla.setBuddyOffline(intent.getStringExtra("alias"), intent
						.getStringExtra("group"));
			} else if (purpose.equals("messageRecieved")) {
				if (!connectionWindowOpen) {
					bla.setBuddyMessageRecieved(intent.getStringExtra("name"));
					Bundle b = new Bundle();
					b.putString("name", intent.getStringExtra("name"));
					b.putString("message", intent.getStringExtra("message"));
					b.putString("username", intent.getStringExtra("username"));
					b.putString("password", intent.getStringExtra("password"));
					b
							.putString("className", intent
									.getStringExtra("className"));
					waiting.add(b);
				}
			} else if (purpose.equals("disconnected")) {
				// TODO Notify of error somehow
				if (cv != null && cv.getConnected()) {
					removeBuddies(cv);
					cv.setConnected(false);
					cv.setText(AMProtocolMapper
							.getName(cv.getConnectionClass())
							+ " : " + cv.getUsername());
				}
			}
		}
	}
	/**
	 * Minor memory cleanup from a hack in ConversationContainer
	 */
	private void clearPrefs() {
		Class<? extends Object> c = ConversationContainer.class;
		SharedPreferences.Editor editor = getSharedPreferences(c.toString() + "windows", 0).edit();
		editor.clear();
		editor.commit();
		editor = getSharedPreferences(c.toString() + "aliases", 0).edit();
		editor.clear();
		editor.commit();
		editor = getSharedPreferences(c.toString() + "tome", 0).edit();
		editor.clear();
		editor.commit();
		editor = getSharedPreferences(c.toString() + "meid", 0).edit();
		editor.clear();
		editor.commit();		
		editor = getSharedPreferences(c.toString() + "editing", 0).edit();
		editor.clear();
		editor.commit();
	}
}