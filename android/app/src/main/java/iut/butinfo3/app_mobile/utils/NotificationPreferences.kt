package iut.butinfo3.app_mobile.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Gère le stockage des préférences liées aux notifications.
 */
class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_HAS_ASKED_PERMISSION = "has_asked_permission"
    }

    /**
     * Enregistre si l'utilisateur a activé ou désactivé les notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    /**
     * Vérifie si les notifications sont activées par l'utilisateur.
     */
    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    /**
     * Marque que la demande de permission système a déjà été présentée au moins une fois.
     */
    fun setPermissionAsked() {
        prefs.edit { putBoolean(KEY_HAS_ASKED_PERMISSION, true) }
    }

    /**
     * Vérifie si l'on a déjà demandé la permission à l'utilisateur.
     */
    fun hasAskedPermission(): Boolean = prefs.getBoolean(KEY_HAS_ASKED_PERMISSION, false)
}