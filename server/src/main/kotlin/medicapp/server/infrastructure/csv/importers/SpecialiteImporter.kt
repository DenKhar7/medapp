package medicapp.server.infrastructure.csv.importers

import medicapp.server.domain.models.MedSpecialite
import medicapp.server.infrastructure.csv.core.CsvImporter
import java.io.File
import java.sql.Connection
import java.sql.Date

object SpecialiteImporter {

    private val sql = """
        INSERT INTO med_specialite (
            cis, uri, libelle, actif, date_debut, date_fin,
            code_atc, libelle_atc, type_procedure, statut_courant,
            titulaire_id, niveau_virtualisation, prescriptibilite_dc, forme_manufacturee
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            uri = VALUES(uri),
            libelle = VALUES(libelle),
            actif = VALUES(actif),
            date_debut = VALUES(date_debut),
            date_fin = VALUES(date_fin),
            code_atc = VALUES(code_atc),
            libelle_atc = VALUES(libelle_atc),
            type_procedure = VALUES(type_procedure),
            statut_courant = VALUES(statut_courant),
            titulaire_id = VALUES(titulaire_id),
            niveau_virtualisation = VALUES(niveau_virtualisation),
            prescriptibilite_dc = VALUES(prescriptibilite_dc),
            forme_manufacturee = VALUES(forme_manufacturee);
    """.trimIndent()

    fun run(csvFile: File, connection: Connection) {
        CsvImporter(
            mapper = { row ->
                try {
                    MedSpecialite(
                        cis = row["CIS"]!!.toInt(),
                        uri = row["URI"] ?: "",
                        libelle = row["libelle"] ?: "",
                        actif = row["actif"]?.equals("true", ignoreCase = true),
                        dateDebut = row["dateDebut"]?.takeIf { it.isNotBlank() }?.let { Date.valueOf(it) },
                        dateFin = row["dateFin"]?.takeIf { it.isNotBlank() }?.let { Date.valueOf(it) },
                        codeATC = row["codeATC"],
                        libelleATC = row["libelleATC"],
                        typeProcedure = row["typeProcedureCourante"],
                        statutCourant = row["statutCourant"],
                        titulaireId = row["titulaireCourant"],
                        niveauVirtualisation = row["niveauDeVirtualisation"],
                        prescriptibiliteDc = row["prescriptibiliteEnDC"],
                        formeManufacturee = row["formeManufacturee"]
                    )
                } catch (_: Exception) {
                    null
                }
            },
            sql = sql,
            binder = { stmt, s ->
                stmt.setInt(1, s.cis)
                stmt.setString(2, s.uri)
                stmt.setString(3, s.libelle)
                stmt.setObject(4, s.actif)
                stmt.setDate(5, s.dateDebut)
                stmt.setDate(6, s.dateFin)
                stmt.setString(7, s.codeATC)
                stmt.setString(8, s.libelleATC)
                stmt.setString(9, s.typeProcedure)
                stmt.setString(10, s.statutCourant)
                stmt.setString(11, s.titulaireId)
                stmt.setString(12, s.niveauVirtualisation)
                stmt.setString(13, s.prescriptibiliteDc)
                stmt.setString(14, s.formeManufacturee)
            },
            batchSize = 500
        ).run(csvFile, connection)
    }
}
