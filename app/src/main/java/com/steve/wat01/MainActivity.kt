package com.steve.wat01

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }

        val url = EditText(this).apply { setText(Prefs.serverUrl(this@MainActivity)); hint = "URL du serveur Gemini" }
        val persona = EditText(this).apply { setText(Prefs.persona(this@MainActivity)); hint = "Profil de réponse" }
        val apps = EditText(this).apply {
            setText(Prefs.apps(this@MainActivity).joinToString(","))
            hint = "Applis à synchroniser, ex : com.whatsapp,com.facebook.orca"
        }
        val auto = Button(this).apply {
            text = if (Prefs.autoReply(this@MainActivity)) "Réponse auto : ON" else "Réponse auto : OFF"
            setOnClickListener {
                Prefs.sp(this@MainActivity).edit().putBoolean("auto", !Prefs.autoReply(this@MainActivity)).apply()
                text = if (Prefs.autoReply(this@MainActivity)) "Réponse auto : ON" else "Réponse auto : OFF"
            }
        }

        listOf(
            TextView(this).apply { text = "Serveur" }, url,
            TextView(this).apply { text = "Paramètres avancés : profil" }, persona,
            TextView(this).apply { text = "Synchronisation" }, apps,
            Button(this).apply {
                text = "Enregistrer"
                setOnClickListener {
                    Prefs.sp(this@MainActivity).edit()
                        .putString("server", url.text.toString().trim())
                        .putString("persona", persona.text.toString())
                        .putStringSet("apps", apps.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet())
                        .apply()
                }
            },
            Button(this).apply {
                text = "Autoriser l'accès aux notifications"
                setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            },
            Button(this).apply {
                text = "Autoriser la bulle flottante"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            },
            Button(this).apply {
                text = "Démarrer Wat01"
                setOnClickListener { startForegroundService(Intent(this@MainActivity, FloatingService::class.java)) }
            },
            auto
        ).forEach { box.addView(it) }

        setContentView(box)
    }
}
