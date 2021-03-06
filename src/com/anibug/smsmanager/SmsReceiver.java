package com.anibug.smsmanager;

import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsMessage;

import com.anibug.smsmanager.model.ContactManager;
import com.anibug.smsmanager.model.Message;
import com.anibug.smsmanager.model.MessageManager;
import com.anibug.smsmanager.utils.PreferenceConstants;

public class SmsReceiver extends BroadcastReceiver {

	public static final String SMS_RECEIVED_ACTION = "com.anibug.smsmanager.SMS_RECEIVED_ACTION";
    public static final int MSG_RECEIVED_NTF = 1;

	@Override
    public void onReceive(Context context, Intent intent) {
		final MessageManager messageManager = new MessageManager(context);
		final ContactManager contactManager = new ContactManager(context);

        final Bundle bundle = intent.getExtras();
        final Object messages[] = (Object[]) bundle.get("pdus");

		final SharedPreferences settings = context.getSharedPreferences(PreferenceConstants.PREF_NAME, Context.MODE_PRIVATE);
		final boolean blocking = settings.getBoolean(MessageManager.PREF_BLOCKING, true);
        final int vibrationDuration = settings.getInt(PreferenceConstants.PREF_VIBRATION_DURATION,
                PreferenceConstants.DEFAULT_VIBRATION_DURATION);

        for (final Object m : messages) {
        	final SmsMessage sms = SmsMessage.createFromPdu((byte[]) m);

        	final Message message = new Message(sms.getDisplayOriginatingAddress(), new Date(),
        			sms.getDisplayMessageBody(), Message.STATUS_RECEIVED);

        	if (contactManager.match(message)) {
        		messageManager.save(message);

        		//send out broadcast to refresh ui                   f
        		final Intent mIntent = new Intent();
        		mIntent.setAction(SMS_RECEIVED_ACTION);
        		context.sendBroadcast(mIntent);

                if (blocking) {
                    abortBroadcast();
                    sendNotification(context);
                    vibrate(context, vibrationDuration);
                }
            }
        }
    }

	public static void sendNotification(Context context) {
		final NotificationManager manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification notification = new Notification(android.R.drawable.stat_sys_download_done,
				context.getString(R.string.update), System.currentTimeMillis());
        notification.flags = notification.flags | Notification.FLAG_SHOW_LIGHTS;

		final Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, settings, 0);
		notification.setLatestEventInfo(context, context.getString(R.string.fake_notification), "", contentIntent);

		manager.notify(MSG_RECEIVED_NTF, notification);
	}

    private void vibrate(Context context, int duration) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(duration);
    }
}