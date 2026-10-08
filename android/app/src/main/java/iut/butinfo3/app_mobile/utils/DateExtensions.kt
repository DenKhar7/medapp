package iut.butinfo3.app_mobile.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension utilisé pour éviter la duplication de code basique
 */
/**
 * Formate un timestamp (millisecondes) en une date lisible pour l'utilisateur français.
 * Utilise le format "dd/MM/yyyy" (Ex: 31/12/2024).
 */
fun Long.toReadableDate(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))
}

/**
 * Extrait l'heure et les minutes d'une chaine de caractères au format "HH:mm".
 * Usage :
 * val (heure, minute) = "14:30".toTimeParts()
 */
fun String.toTimeParts(): Pair<Int, Int> {
    if (!this.contains(":")) return 12 to 0
    val parts = this.split(":")
    return parts[0].toInt() to parts[1].toInt()
}