package medicapp.server.infrastructure.jobs

object JobsConstants {
    private const val SMT_BASE_URL = "https://smt.esante.gouv.fr"

    // RUIM Constantes
    const val RUIM_URL = "$SMT_BASE_URL/terminologie-ref_interop_med/"
    private const val RUIM_TERMINOLOGY_ID = "terminologie-ref_interop_med"

    // SMS Constantes
    const val SMS_URL = "$SMT_BASE_URL/terminologie-sms/"
    private const val SMS_TERMINOLOGY_ID = "terminologie-sms"

    /**
     * Construit l'URL de téléchargement du ZIP d'une terminologie pour une version donnée.
     *
     * La version doit être celle lue sur la page HTML du référentiel (ex : "2026-09"), afin que
     * les données téléchargées correspondent toujours à la version enregistrée en base.
     */
    fun downloadUrl(terminologyId: String, version: String): String =
        "$SMT_BASE_URL/wp-json/ans/terminologies/zip" +
                "?terminologyId=$terminologyId" +
                "&version=$version" +
                "&licenceConsent=true&dataTransferConsent=true&sizeConsent=false"

    fun ruimDownloadUrl(version: String): String = downloadUrl(RUIM_TERMINOLOGY_ID, version)

    fun smsDownloadUrl(version: String): String = downloadUrl(SMS_TERMINOLOGY_ID, version)
}
