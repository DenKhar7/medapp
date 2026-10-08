package iut.butinfo3.app_mobile.utils

import android.widget.ImageView

/**
 * NavigationHelper - Gère l'état visuel de la barre de navigation
 * Évite le rechargement inutile et met en évidence l'onglet actif
 */
class NavigationHelper(
    private val navHome: ImageView,
    private val navSettings: ImageView,
    private val navMedicaments: ImageView,
    private val navOrdonnances: ImageView,
    private val navPathologie: ImageView
) {

    companion object {
        const val NAV_HOME = 0
        const val NAV_SETTINGS = 1
        const val NAV_MEDICAMENTS = 2
        const val NAV_ORDONNANCES = 3
        const val NAV_PATHOLOGIE = 4
    }

    private var currentTab = -1

    /**
     * Initialise la navigation avec l'onglet Home actif par défaut
     */
    fun init(activeTab: Int = NAV_HOME) {
        setActiveTab(activeTab)
    }

    /**
     * Définit l'onglet actif et met à jour l'apparence
     */
    fun setActiveTab(tabIndex: Int) {
        if (isTabActive(tabIndex)) {
            return
        }
        resetAllTabs()

        when (tabIndex) {
            NAV_HOME -> highlightTab(navHome)
            NAV_SETTINGS -> highlightTab(navSettings)
            NAV_MEDICAMENTS -> highlightTab(navMedicaments)
            NAV_PATHOLOGIE -> highlightTab(navPathologie)
            NAV_ORDONNANCES -> highlightTab(navOrdonnances)
        }

        currentTab = tabIndex
    }

    /**
     * Vérifie si un onglet est actuellement actif
     */
    fun isTabActive(tabIndex: Int): Boolean {
        return currentTab == tabIndex
    }

    /**
     * Réinitialise l'apparence de tous les onglets
     */
    private fun resetAllTabs() {
        navHome.alpha = 0.5f
        navSettings.alpha = 0.5f
        navMedicaments.alpha = 0.5f
        navPathologie.alpha = 0.5f
        navOrdonnances.alpha = 0.5f

        navHome.clearColorFilter()
        navSettings.clearColorFilter()
        navMedicaments.clearColorFilter()
        navPathologie.clearColorFilter()
        navOrdonnances.clearColorFilter()
    }

    /**
     * Met en évidence un onglet actif
     */
    private fun highlightTab(tab: ImageView) {
        tab.alpha = 1.0f
    }
}