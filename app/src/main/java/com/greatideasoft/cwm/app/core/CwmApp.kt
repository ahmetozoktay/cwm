package com.greatideasoft.cwm.app.core

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greatideasoft.cwm.data.core.log.DebugConfig
import com.greatideasoft.cwm.app.R
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class CwmApp : Application() {
	
	companion object {
		val debug: DebugConfig = DebugConfig.Default
	}

	override fun onCreate() {
		super.onCreate()
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!debug.isEnabled)
		FirebaseAnalytics.getInstance(this)

		val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			val messageNotificationChannel =
				NotificationChannel(
					getString(R.string.notification_channel_id_messages),
					getString(R.string.notification_channel_name_messages),
					NotificationManager.IMPORTANCE_DEFAULT
				).apply { description = getString(R.string.notification_channel_description_messages) }

			val matchNotificationChannel =
				NotificationChannel(
					getString(R.string.notification_channel_id_match),
					getString(R.string.notification_channel_name_match),
					NotificationManager.IMPORTANCE_DEFAULT
				).apply { description = getString(R.string.notification_channel_description_match) }
			notificationManager.createNotificationChannel(messageNotificationChannel)
			notificationManager.createNotificationChannel(matchNotificationChannel)
		}

	}
}
