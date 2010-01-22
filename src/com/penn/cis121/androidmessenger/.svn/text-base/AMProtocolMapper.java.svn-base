/**
 * @author - Zachary Goldberg @ 2008
 */
package com.penn.cis121.androidmessenger;

import java.util.HashMap;

import com.penn.cis121.androidmessenger.protocols.*;

public class AMProtocolMapper {
	public static HashMap<Class<? extends Object>,String> names = new HashMap<Class<? extends Object>,String>();

	public static void addMapping(Class<? extends Object> c, String connectionTypeName){
		names.put(c,connectionTypeName);
	}
	public static String getName(Class<? extends Object> c){
		return names.get(c);
	}
	public static HashMap<Class<? extends Object>, String> getAll(){
		return names;
	}
	
}
