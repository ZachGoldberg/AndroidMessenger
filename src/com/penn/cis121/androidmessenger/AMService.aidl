package com.penn.cis121.androidmessenger;

interface AMService{

   void Connect(String username, String password, int connectionId);
   void sendMessage(String buddyName, String message);
}