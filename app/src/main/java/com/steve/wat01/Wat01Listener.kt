package com.steve.wat01

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlin.concurrent.thread

data class Msg(val pkg: String, val title: String, val text: String)

object Inbox {
    private val items = ArrayDeque<Msg>()
    fun add(m: Msg) = synchronized(items) {
        items.addFirst(m)
        while (items.size > 50) items.removeLast()
    }
    fun last(): Msg? = synchronized(items) { items.firstOrNull() }
}

class Wat01Listener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.packageName !in Prefs.apps(this)) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        Inbox.add(Msg(sbn.packageName, title, text))
        startService(Intent(this, FloatingService::class.java).setAction(FloatingService.ACTION_REFRESH))
        if (Prefs.autoReply(this)) autoReply(sbn, text)
    }

    private fun autoReply(sbn: StatusBarNotification, text: String) {
        val action = sbn.notification.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return
        thread {
            val reply = try {
                GeminiClient.ask(this, "Message reçu : $text\nRéponds à ce message.")
            } catch (e: Exception) {
                return@thread
            }
            val ri = action.remoteInputs[0]
            val bundle = Bundle().apply { putCharSequence(ri.resultKey, reply) }
            val intent = Intent()
            RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
            try {
                action.actionIntent.send(this, 0, intent)
            } catch (_: PendingIntent.CanceledException) {
            }
        }
    }
}
