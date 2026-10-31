package com.steve.wat01

import android.content.Context

object Prefs {
    const val DEFAULT_PERSONA = "Je m'appelle Steve, entrepreneur, 28 ans. J'aime créer et étudier de nouvelles choses. Je ne parle pas de ma famille. Réponds en français, brièvement, comme moi."

    fun sp(c: Context) = c.getSharedPreferences("wat01", Context.MODE_PRIVATE)
    fun serverUrl(c: Context): String = sp(c).getString("server", "https://VOTRE-SERVEUR.onrender.com")!!
    fun persona(c: Context): String = sp(c).getString("persona", DEFAULT_PERSONA)!!
    fun autoReply(c: Context): Boolean = sp(c).getBoolean("auto", false)
    fun apps(c: Context): MutableSet<String> = sp(c).getStringSet("apps", emptySet())!!.toMutableSet()
}
