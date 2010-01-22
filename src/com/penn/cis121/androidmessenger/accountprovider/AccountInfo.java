/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger.accountprovider;

import android.net.Uri;
import android.provider.BaseColumns;


public final class AccountInfo {
    /**
     * Notes table
     */
    public static final class Account implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
    	public static final Uri CONTENT_URI = Uri
    		.parse("content://com.penn.cis121.androidmessenger.accountprovider/accounts");	

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "";
        /**
         * The username belonging to the account
         * <P>Type: TEXT</P>
         */
        public static final String USERNAME = "username";
        /**
         * The password belonging to the account
         * <P>Type: TEXT</P>
         */
        public static final String PASSWORD = "password";
        /**
         * The classname belonging to the account
         * <P>Type: TEXT</P>
         */
        public static final String CLASSNAME = "className";        
    }
}