/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger.accountprovider;

import java.util.HashMap;
import java.util.List;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class AccountProvider extends ContentProvider {
	
	/* Constants */
	private static final String TAG = "AccountProvider";
    private static final String DATABASE_NAME = "accountprovider.db";
    private static final int DATABASE_VERSION = 2;
	private static final int ACCOUNT = 0;
	private static final int ACCOUNTS = 1;
    
	
	/* Private Variables */
	private static UriMatcher URL_MATCHER;
	private SQLiteDatabase mDB;
	private static HashMap<String, String> ACCOUNTS_PROJECTION_MAP;
	
	static {
		URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URL_MATCHER.addURI("com.penn.cis121.androidmessenger.accountprovider", "accounts", ACCOUNTS);
		URL_MATCHER.addURI("com.penn.cis121.androidmessenger.accountprovider", "accounts/#", ACCOUNT);
		ACCOUNTS_PROJECTION_MAP = new HashMap<String, String>();
		ACCOUNTS_PROJECTION_MAP.put(AccountInfo.Account._ID, "_id");
		ACCOUNTS_PROJECTION_MAP.put(AccountInfo.Account.USERNAME, "username");
		ACCOUNTS_PROJECTION_MAP.put(AccountInfo.Account.PASSWORD, "password");
		ACCOUNTS_PROJECTION_MAP.put(AccountInfo.Account.CLASSNAME, "classname");
	}
	
	public AccountProvider() {
		super();
	}

	@Override
	public String getType(Uri uri) {
		switch (URL_MATCHER.match(uri)) {
		case ACCOUNTS:
			return "vnd.android.cursor.dir/vnd.google.account";

		case ACCOUNT:
			return "vnd.android.cursor.item/vnd.google.account";
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	private static class DatabaseHelper extends SQLiteOpenHelper{

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE accounts (_id INTEGER PRIMARY KEY,"
					+ "username TEXT," + "password TEXT," + "className TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS accounts");
			onCreate(db);
		}
	}

	@Override
	public boolean onCreate() {
		DatabaseHelper dbHelper = new DatabaseHelper();
		mDB = dbHelper.openDatabase(getContext(), DATABASE_NAME, null,
				DATABASE_VERSION);
		return (mDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
				String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (URL_MATCHER.match(uri)) {
		case ACCOUNTS:
			qb.setTables("accounts");
			qb.setProjectionMap(ACCOUNTS_PROJECTION_MAP);
			break;

		case ACCOUNT:
			qb.setTables("accounts");
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;	
		orderBy = AccountInfo.Account.DEFAULT_SORT_ORDER;

		Cursor c = qb.query(mDB, projection, selection, selectionArgs, "",
				"", orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		long rowID;
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		if (URL_MATCHER.match(url) != ACCOUNTS) {
			throw new IllegalArgumentException("Unknown URL " + url +"," + URL_MATCHER.match(url) + "," + ACCOUNTS);
		}

		rowID = mDB.insert("accounts", "account", values);
		if (rowID > 0) {					
			Uri uri = ContentUris.appendId(AccountInfo.Account.CONTENT_URI.buildUpon(),rowID).build();
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}

		throw new SQLException("Failed to insert row into " + url);
	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		int count;
		long rowId = 0;
		switch (URL_MATCHER.match(url)) {
		case ACCOUNTS:
			count = mDB.delete("accounts", where, whereArgs);
			break;

		case ACCOUNT:
			List<String> segments = url.getPathSegments();
			String segment = segments.get(1);
			rowId = Long.parseLong(segment);
			count = mDB.delete("accounts",
					"_id="
							+ segment
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}

	@Override
	public int update(Uri url, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		switch (URL_MATCHER.match(url)) {
		case ACCOUNTS:
			count = mDB.update("accounts", values, where, whereArgs);
			break;

		case ACCOUNT:
			
			List<String> segment = url.getPathSegments();
			count = mDB.update("accounts", values,
					"_id="
							+ segment
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}
}