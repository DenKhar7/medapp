package iut.butinfo3.app_mobile.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Classe utilisée pour vérifier que des éléments textes soit valides:
 */
class ValidateText {

    /**
     * Vérifie si l'adresse mail est conforme au pattern définit par android
     */
    fun checkEmailValid(email:String) : Boolean{
        if (email.isEmpty()) {
            return false
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Vérifier qu'un mot de passe possède bien au moins 8 caractères, 1 MAJ, 1 MIN et 1 caractère spécial.
     */
    fun checkPSWValid(pwd: String): Boolean {
        if (pwd.length < 8) {
            return false
        }
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}\$"
        return Pattern.matches(pattern, pwd)
    }

    /**
     * Vérifier que le nom ne possède pas de caractères spéciaux
     */
    fun checkNameValid(name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        if (name.trim().length < 2) {
            return false
        }
        val pattern = "^[a-zA-ZÀ-ÿ\\s'-]+\$"
        return Pattern.matches(pattern, name.trim())
    }
}