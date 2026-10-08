package iut.butinfo3.app_mobile.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


/**
 * Utilitaires de cryptographie pour la sécurisation des mots de passe.
 *
 * Pourquoi cette classe ?
 * Il est strictement INTERDIT de stocker des mots de passe en clair dans une base de données.
 * Cette classe transforme un mot de passe (ex: "Azerty123") en une empreinte impossible à inverser (le Hachage).
 *
 * L'Algorithme : PBKDF2
 * - Un **Sel (Salt)** aléatoire : deux utilisateurs avec le même mot de passe ont des empreintes différentes.
 * - Beaucoup d'itérations : calcul volontairement lent pour rendre le test massif de mots de passe coûteux.
 *
 * Format des empreintes stockées :
 * - **v2** (actuel) : `v2$600000$<base64>`  -> PBKDF2-HMAC-SHA256, 600 000 itérations, clé de 256 bits
 *   (recommandation OWASP). Le nombre d'itérations est écrit dans l'empreinte : on pourra l'augmenter plus tard
 *   sans casser les comptes existants.
 * - **legacy** (anciennes versions de l'app) : base64 brut -> PBKDF2-HMAC-SHA1, 65 536 itérations, 128 bits.
 *   Toujours vérifiable ; [needsRehash] indique qu'il faut recalculer l'empreinte à la prochaine connexion réussie.
 */
class SecurityUtils {

    /**
     * Génère une chaîne aléatoire unique (le Sel).
     *
     * À appeler UNIQUEMENT lors de la création d'un compte ou du changement de mot de passe.
     * Ce sel doit être stocké en clair dans la db à côté du hash.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Transforme le mot de passe en empreinte sécurisée (format v2).
     * C'est une fonction à sens unique : on ne peut pas retrouver le mot de passe depuis l'empreinte.
     */
    fun hashPassword(password: String, salt: String): String {
        val derived = pbkdf2(password, salt, ALGORITHM_V2, ITERATIONS_V2, KEY_BITS_V2)
        return "$PREFIX_V2$ITERATIONS_V2$SEPARATOR${Base64.encodeToString(derived, Base64.NO_WRAP)}"
    }

    /**
     * Vérifie si un mot de passe correspond à l'empreinte stockée (v2 ou legacy).
     * La comparaison se fait en temps constant pour ne rien révéler sur l'empreinte.
     */
    fun verifyPassword(inputPassword: String, storedSalt: String, storedHash: String): Boolean {
        val expected = if (isV2(storedHash)) {
            val iterations = storedHash.removePrefix(PREFIX_V2).substringBefore(SEPARATOR).toIntOrNull()
                ?: return false
            val derived = pbkdf2(inputPassword, storedSalt, ALGORITHM_V2, iterations, KEY_BITS_V2)
            "$PREFIX_V2$iterations$SEPARATOR${Base64.encodeToString(derived, Base64.NO_WRAP)}"
        } else {
            val derived = pbkdf2(inputPassword, storedSalt, ALGORITHM_LEGACY, ITERATIONS_LEGACY, KEY_BITS_LEGACY)
            Base64.encodeToString(derived, Base64.NO_WRAP)
        }
        return MessageDigest.isEqual(expected.toByteArray(), storedHash.toByteArray())
    }

    /** Vrai si l'empreinte stockée n'est pas au format actuel (ou utilise moins d'itérations que l'actuel). */
    fun needsRehash(storedHash: String): Boolean {
        if (!isV2(storedHash)) return true
        val iterations = storedHash.removePrefix(PREFIX_V2).substringBefore(SEPARATOR).toIntOrNull() ?: return true
        return iterations < ITERATIONS_V2
    }

    private fun isV2(hash: String) = hash.startsWith(PREFIX_V2)

    private fun pbkdf2(password: String, salt: String, algorithm: String, iterations: Int, keyBits: Int): ByteArray {
        val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), iterations, keyBits)
        return SecretKeyFactory.getInstance(algorithm).generateSecret(keySpec).encoded
    }

    private companion object {
        const val PREFIX_V2 = "v2$"
        const val SEPARATOR = "$"

        const val ALGORITHM_V2 = "PBKDF2WithHmacSHA256"
        const val ITERATIONS_V2 = 600_000
        const val KEY_BITS_V2 = 256

        const val ALGORITHM_LEGACY = "PBKDF2WithHmacSHA1"
        const val ITERATIONS_LEGACY = 65_536
        const val KEY_BITS_LEGACY = 128
    }
}
