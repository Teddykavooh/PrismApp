package com.prisms.smsapp1;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Date;
import java.util.Objects;

//Responsible for message intercepting.
public class SmsBroadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_SMS_NEW = "android.provider.Telephony.SMS_RECEIVED";
    private String address;
    private long smsTime;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent) {
        String myApp = context.getPackageName();
        MainActivity inst = MainActivity.instance();
        Log.e("SMS.onReceive ", myApp + "\n" + Telephony.Sms.getDefaultSmsPackage(context));
        inst.refreshSmsInbox();
//        Bundle intentExtras = intent.getExtras();
//
//        if (intentExtras != null) {
//            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
//            //StringBuilder smsMessageStr = new StringBuilder();
//            ContentValues values = new ContentValues();
//            for (Object sm : Objects.requireNonNull(sms)) {
//                String format = intentExtras.getString("format");
//                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sm, format);
//
//                /*String smsBody = smsMessage.getMessageBody();
//                String address = smsMessage.getOriginatingAddress();
//                int smsIndex = smsMessage.getIndexOnIcc();
//                long smsTime = smsMessage.getTimestampMillis();
//                Date date = new Date(smsTime);*/
//
//                /*smsMessageStr.append("REF: ").append(smsIndex).append("\n");
//                smsMessageStr.append("From: ").append(address).append("\n");
//                smsMessageStr.append(smsBody).append("\n");
//                smsMessageStr.append("Date: ").append(date).append("\n");*/
//
//                //Save to inbox if message is delivered
//                final String action = intent.getAction();
//                if (ACTION_SMS_NEW.equals(action) /*Default app activity*/
//                && myApp.equals(Telephony.Sms.getDefaultSmsPackage(context))) {
//                    values.put("address", smsMessage.getOriginatingAddress()); // phone number to send
//                    values.put("date", smsMessage.getTimestampMillis());
//                    values.put("read", "1"); // if you want to mark it as unread set to 0
//                    values.put("type", "1"); // 2 means sent message
//                    values.put("body", smsMessage.getMessageBody());
//                    Uri uri = Uri.parse("content://sms/");
//                    context.getContentResolver().insert(uri, values);
//                    //Log.e("SmsBroadcast0nReceive: ", "We passed here");
//                }
//            }
//
//            /* Notification Tone */
//            try {
//                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
//                r.play();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            /* Refresh inbox after upload */
//            MainActivity inst = MainActivity.instance();
//            inst.refreshSmsInbox();
//
//        }

        /*New logic*/
        if (Objects.equals(intent.getAction(), ACTION_SMS_NEW)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // get sms objects
                Object[] pdus = (Object[]) bundle.get("pdus");
                assert pdus != null;
                if (pdus.length == 0) {
                    return;
                }
                SmsMessage[] messages = new SmsMessage[pdus.length];
                ContentValues values = new ContentValues();
//                StringBuilder smsMessageStr = new StringBuilder();
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    address = messages[i].getOriginatingAddress();
                    smsTime = messages[i].getTimestampMillis();
                }

                // If SMS has several parts, let's combine it
                StringBuilder bodyText = new StringBuilder();
                for (SmsMessage message : messages) {
                    bodyText.append(message.getMessageBody());
                }
//                Log.e("Auto Print SMS Broad...", "New sms body: " + bodyText);
//                smsMessageStr.append("REF: ").append(smsIndex).append("\n");
//                smsMessageStr.append("From: ").append(address).append("\n");
//                smsMessageStr.append(bodyText).append("\n");
//                smsMessageStr.append("Date: ").append(smsTime).append("\n");

//                Log.e("Auto Print SMS Broad...", "sms body: " + bodyText);
//                Log.e("SmsBroadcast:onReceive: ", "Sender: " + address);

//                MainActivity inst = MainActivity.instance();
//                inst.refreshSmsInbox();
//                inst.updateInbox(smsMessageStr.toString());
//                inst.refreshSmsInbox();

                /*Save to inbox if message is received*/
                if (myApp.equals(Telephony.Sms.getDefaultSmsPackage(context))) {
                    values.put("address", address); // phone number to send
                    values.put("date", smsTime);
                    values.put("read", "1"); // if you want to mark it as unread set to 0
                    values.put("type", "1"); // 2 means sent message
                    values.put("body", String.valueOf(bodyText));
                    Uri uri = Uri.parse("content://sms/");
                    context.getContentResolver().insert(uri, values);
                }
            }

            /* Notification Tone */
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Refresh inbox after upload */
            //inst.updateInboxN();
            inst.refreshSmsInbox();

            /*Auto Print Comes in*/
            inst.autoP(smsTime);
            //inst.autoPrint();
        }
    }
}