/*------------------------------------------------------------------------------
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is levelonelabs.com code.
 * The Initial Developer of the Original Code is Level One Labs. Portions
 * created by the Initial Developer are Copyright (C) 2001 the Initial
 * Developer. All Rights Reserved.
 *
 *         Contributor(s):
 *             Scott Oster      (ostersc@alum.rpi.edu)
 *             Steve Zingelwicz (sez@po.cwru.edu)
 *             William Gorman   (willgorman@hotmail.com)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this
 * file only under the terms of either the GPL or the LGPL, and not to allow
 * others to use your version of this file under the terms of the NPL, indicate
 * your decision by deleting the provisions above and replace them with the
 * notice and other provisions required by the GPL or the LGPL. If you do not
 * delete the provisions above, a recipient may use your version of this file
 * under the terms of any one of the NPL, the GPL or the LGPL.
 *----------------------------------------------------------------------------*/

package com.levelonelabs.aim.example;

import com.levelonelabs.aim.AIMAdapter;
import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMClient;
import com.levelonelabs.aim.AIMSender;


/**
 * Example of the simplicity of the new aim code
 * 
 * @author Scott Oster
 * 
 * @created January 12, 2002
 */
public class EchoExample {
	/**
	 * The main program for the EchoExample class Starts up a aimclient and
	 * signs it on. Then registers a listener that gets called when someone
	 * messages the bot. It simply echos back what they said.
	 * 
	 * @param args
	 *            username password
	 */
	public static void main(String[] args) {
		//username, password, true indicates to add all that talk to it to the
		// buddy list
		final AIMSender aim = new AIMClient(args[0], args[1], "Simple Echo Bot", true);
		aim.addAIMListener(new AIMAdapter() {
			public void handleMessage(AIMBuddy buddy, String request) {
				aim.sendMessage(buddy, "Echo: " + request);
			}
		});
		aim.signOn();
	}
}