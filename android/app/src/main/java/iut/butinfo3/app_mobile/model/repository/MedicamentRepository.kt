package iut.butinfo3.app_mobile.model.repository

import iut.butinfo3.app_mobile.model.api.MedicamentApiClient
import iut.butinfo3.app_mobile.model.dao.MedicamentDao
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume

class MedicamentRepository(
    private val dao: MedicamentDao,
    private val apiClient: MedicamentApiClient
) {

    suspend fun searchByLibelle(libelle: String, limit: Int = 10): List<MedicamentPresDetail> {
        val results = apiClient.searchPresentations(libelle, limit)

        for (presentation in results) {
            val cached = dao.getByCip(presentation.cip13)
            if (cached == null) {
                val existingSpe = dao.getSpeByCis(presentation.cis)
                if (existingSpe == null) {
                    val spe = apiClient.getSpecialiteByCis(presentation.cis)
                    dao.insert(spe)
                }
                dao.insert(presentation)
            }
        }

        return results
    }

    suspend fun searchSpecialites(libelle: String, limit: Int = 10): List<MedicamentSpeResume> {
        return apiClient.searchSpecialites(libelle, limit)
    }

    suspend fun getPresentationsByCis(cis: Int): List<MedicamentPresDetail> {
        // First, try to get from local database
        val localPresentations = dao.getPresentationsByCis(cis)
        if (localPresentations.isNotEmpty()) {
            return localPresentations
        }

        // If not in local DB, fetch from API and store
        val apiPresentations = apiClient.getPresentationsByCis(cis)
        for (presentation in apiPresentations) {
            val cached = dao.getByCip(presentation.cip13)
            if (cached == null) {
                val existingSpe = dao.getSpeByCis(presentation.cis)
                if (existingSpe == null) {
                    val spe = apiClient.getSpecialiteByCis(presentation.cis)
                    dao.insert(spe)
                }
                dao.insert(presentation)
            }
        }
        return apiPresentations
    }
}
