/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;


import com.penn.cis121.androidmessenger.accountprovider.AccountInfo;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class AccountEditor extends Activity {

	private EditText username;
	private EditText password;
	private Spinner connection;
	private Button editButton;
	private Button deleteButton;
	private String ouser, opassword, oclassName;
	private HashMap <String,String> names;
	private Logger logger = Logger.getLogger(AccountEditor.class.getName());
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.accounteditor);

		username = (EditText) findViewById(R.id.edit_username);
		password = (EditText) findViewById(R.id.edit_password);
		connection = (Spinner) findViewById(R.id.chose_connection);
		editButton = (Button) findViewById(R.id.edit_acocunt_button);
		deleteButton = (Button) findViewById(R.id.delete_acocunt_button);
		editAccountCallback ocl = new editAccountCallback();
		editButton.setOnClickListener(ocl);
		deleteButton.setOnClickListener(ocl);
		HashMap<Class<? extends Object>, String> all = AMProtocolMapper
				.getAll();
		names = new HashMap<String, String>();
		
		Intent i = getIntent();		
		int cursor = 0;
		String inName = i.getStringExtra("className");
		for (Class<? extends Object> c : all.keySet()) {
			names.put(all.get(c), c.getName().toString());
			if (inName != null && inName.equals( c.getName().toString())) {
				connection.setSelection(cursor);
				oclassName = c.getName().toString();
			}
			cursor++;
		}
		ProgrammaticSpinnerAdapter paa = new ProgrammaticSpinnerAdapter(this,
				names);
		logger.severe("Inside Account Editor");
		// paa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		connection.setAdapter(paa);


		if (i.getExtras() != null) {
			username.setText( i.getStringExtra("username"));
			password.setText( i.getStringExtra("password"));

			ouser =  i.getStringExtra("username");
			opassword = i.getStringExtra("password");
		}else{
			deleteButton.setVisibility(View.INVISIBLE);
			editButton.setText("Add Account");
		}

	}

	private class editAccountCallback implements OnClickListener {
		public void onClick(View button) {
			logger.severe("Inside callback");
			Button b = (Button) button;
			// Because we don't currently have a cursor to modify, its easier to
			// delete then insert
			if(ouser != null)
				getContentResolver().delete(
					AccountInfo.Account.CONTENT_URI,
					AccountInfo.Account.USERNAME + " = \"" + ouser + "\" AND "
							+ AccountInfo.Account.PASSWORD + " = \"" + opassword
							+ "\" AND " + AccountInfo.Account.CLASSNAME + " = \""
							+ oclassName +"\"", null);
			logger.severe(b.getText().toString());
			logger.severe("Account" + username.getText().toString());
			if (b == editButton) {
				ContentValues values = new ContentValues();
				values.put(AccountInfo.Account.USERNAME, username.getText().toString());
				values.put(AccountInfo.Account.PASSWORD, password.getText().toString());
				values.put(AccountInfo.Account.CLASSNAME, names.get(((TextView) connection.getSelectedView()).getText().toString()));
				getContentResolver().insert(AccountInfo.Account.CONTENT_URI, values);
			}
			finish();
		}
	}

	private class ProgrammaticSpinnerAdapter implements SpinnerAdapter {

		// private HashMap<String,String> nameValues;
		private Context c;
		private LinearLayout vg;
		private List<View> views;
		private int count;

		public ProgrammaticSpinnerAdapter(Context c,
				HashMap<String, String> nameValues) {
			this.c = c;
			// this.nameValues = nameValues;
			views = new ArrayList<View>();
			for (String s : nameValues.keySet()) {
				TextView t = new NameValueView(this.c, nameValues.get(s));
				t.setText(s);
				t.setLayoutParams(new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				views.add(t);
			}
			count = views.size();
			vg = new LinearLayout(this.c);
			vg.setOrientation(LinearLayout.VERTICAL);
		}

		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			// WidgetInflate w = new WidgetInflate(c);
			// View v = w.inflate(android.R.layout.simple_spinner_dropdown_item,
			// vg, null);
			return getView(position, null, null);

		}

		public View getMeasurementView(ViewGroup arg0) {
			// Narf?
			return null;
		}

		public int getCount() {
			return count;
		}

		public Object getItem(int position) {
			return views.get(position);
		}

		public long getItemId(int position) {
			return views.get(position).getId();
		}

		public int getNewSelectionForKey(int currentSelection, int keyCode,
				KeyEvent event) {
			// Hum?
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			return views.get(position);
		}

		public void registerContentObserver(ContentObserver observer) {
			// We don't care right now
		}

		public void registerDataSetObserver(DataSetObserver observer) {
			// Again, we don't care much
		}

		public void unregisterContentObserver(ContentObserver observer) {
			// Not. Caring.
		}

		public void unregisterDataSetObserver(DataSetObserver arg0) {
			// Not. Caring.
		}

		public boolean stableIds() {
			// The fuck?
			return false;
		}
	}

	private class NameValueView extends TextView {
		private String value;

		public NameValueView(Context c, String value) {
			super(c);
		}

		public String getValue() {
			return value;
		}
	}
}
