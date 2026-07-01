package com.dmgproductions.amp.streaming

import android.service.notification.NotificationListenerService

/**
 * Minimal notification listener. Its sole purpose is to let A.M.P read the
 * device's active media sessions via MediaSessionManager once the user grants
 * "Notification access" — it does not read or store notification content.
 */
class AmpNotificationListenerService : NotificationListenerService()
