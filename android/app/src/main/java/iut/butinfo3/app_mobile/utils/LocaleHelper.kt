package iut.butinfo3.app_mobile.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import androidx.core.content.edit

object LocaleHelper {
    private const val PREFS_NAME = "AppSettings"
    private const val PREFS_LANGUAGE = "language"

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.setLocale(locale)
        
        return context.createConfigurationContext(configuration)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_LANGUAGE, "fr") ?: "fr"
    }

    fun saveLanguage(context: Context, language: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(PREFS_LANGUAGE, language) }
    }

    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            "fr" -> "Français"
            "en" -> "English"
            else -> "Français"
        }
    }
}


