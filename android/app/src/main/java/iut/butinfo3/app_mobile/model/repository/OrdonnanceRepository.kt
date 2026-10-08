package iut.butinfo3.app_mobile.model.repository

import iut.butinfo3.app_mobile.model.dao.OrdonnanceDao
import iut.butinfo3.app_mobile.model.entity.Ordonnance
import kotlinx.coroutines.flow.Flow
import java.io.File

class OrdonnanceRepository(private val ordonnanceDao: OrdonnanceDao) {

    fun getOrdonnancesByUser(userId: Int): Flow<List<Ordonnance>> {
        return ordonnanceDao.getOrdonnancesByUser(userId)
    }

    suspend fun getOrdonnanceById(ordonnanceId: Int): Ordonnance? {
        return ordonnanceDao.getOrdonnanceById(ordonnanceId)
    }

    suspend fun insertOrdonnance(ordonnance: Ordonnance): Long {
        return ordonnanceDao.insertOrdonnance(ordonnance)
    }

    suspend fun updateOrdonnance(ordonnance: Ordonnance) {
        ordonnanceDao.updateOrdonnance(ordonnance)
    }

    suspend fun deleteOrdonnance(ordonnance: Ordonnance) {
        ordonnanceDao.deleteOrdonnance(ordonnance)
        deleteScannedImage(ordonnance.imagePath)
    }

    /**
     * Supprime la photo de l'ordonnance (donnée de santé) avec l'ordonnance. Seuls les fichiers créés par
     * l'application (préfixe ordonnance_scan_) sont supprimés, jamais un chemin arbitraire.
     */
    private fun deleteScannedImage(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.name.startsWith(CameraRepository.SCAN_PREFIX)) {
            runCatching { file.delete() }
        }
    }
}