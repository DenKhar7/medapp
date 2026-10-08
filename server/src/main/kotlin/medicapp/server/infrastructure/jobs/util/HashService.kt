package medicapp.server.infrastructure.jobs.util

import java.io.File
import java.security.MessageDigest

object HashService {

    /**
     * Calcule le hash SHA-256 du contenu d'un fichier.
     *
     * @param file le fichier dont on veut calculer le hash
     * @return le hash SHA-256 du contenu du fichier en hexadécimal
     */
    fun computeHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compare le hash SHA-256 d'un fichier avec un hash de référence.
     *
     * @param file le fichier dont on veut comparer le hash
     * @param referenceHash le hash de référence à comparer
     * @return true si les deux hash sont identiques, false sinon
     */
    fun isSame(file: File, referenceHash: String): Boolean {
        return computeHash(file) == referenceHash
    }
}
