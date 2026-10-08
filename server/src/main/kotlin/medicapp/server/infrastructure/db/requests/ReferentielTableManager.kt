package medicapp.server.infrastructure.db.requests

import medicapp.server.config.MedDatabase
import medicapp.server.infrastructure.db.enums.DataSourceEnum
import medicapp.server.infrastructure.db.enums.TableEnum
import medicapp.server.infrastructure.db.requests.dao.TableMaintenanceDAO

object ReferentielTableManager {

    /** Table "témoin" par référentiel, utilisée pour vérifier qu'un import n'est pas vide ou tronqué. */
    private val sentinelTable = mapOf(
        DataSourceEnum.RUIM to TableEnum.SPECIALITE,
        DataSourceEnum.SMS to TableEnum.DICTIONNAIRE_SMS
    )

    /**
     * Méthode permettant de supprimer toutes les tables dans la base de donnée med_db_archive
     * en fonction du référentiel qui est mis à jour.
     * IMPORTANT : Cette méthode ne doit être utilisé que dans un contexte de maintenance
     * @param dataSource Référentiel mis à jour
     */
    fun dropTablesArchive(
        dataSource : DataSourceEnum
    ) {
        val tablesToDelete = TableEnum.entries
            .filter { it.source == dataSource }
            .sortedBy { it.order }

        TableMaintenanceDAO.dropTablesArchiveForce(tablesToDelete)
    }

    /**
     * Vérifie que les données fraîchement importées dans med_db_temp sont plausibles avant de les
     * mettre en production : la table témoin ne doit pas être vide, ni avoir perdu plus de la moitié de
     * ses lignes par rapport à med_db_active. Protège contre un fichier source tronqué ou mal formé.
     *
     * @throws IllegalStateException si les données importées ne sont pas plausibles
     */
    fun assertImportIsSane(
        dataSource: DataSourceEnum,
        minRatio: Double = 0.5
    ) {
        val table = sentinelTable.getValue(dataSource)
        val fresh = TableMaintenanceDAO.countRows(table, MedDatabase.TEMP)
        val current = TableMaintenanceDAO.countRows(table, MedDatabase.ACTIVE)

        check(fresh > 0) { "Import $dataSource rejeté : ${table.tableName} est vide dans med_db_temp" }
        check(current == 0L || fresh >= current * minRatio) {
            "Import $dataSource rejeté : ${table.tableName} passe de $current à $fresh ligne(s) (< ${(minRatio * 100).toInt()} %)"
        }
    }

    /**
     * Cette méthode permet d'appliquer la logique de roulement de version.
     * Si l'importation des données s'est bien passé, on supprime les tables
     * dans med_db_archive (en fonction du référentiel mis à jour).
     * On bascule ensuite, en UNE SEULE instruction atomique, les tables de med_db_active vers
     * med_db_archive et celles de med_db_temp vers med_db_active, puis on recrée des tables vides
     * dans med_db_temp pour le prochain import.
     * @param dataSource Référentiel mis à jour
     */
    fun switchTables(
        dataSource : DataSourceEnum
    ) {
        val tablesToSwitch = TableEnum.entries
            .filter { it.source == dataSource }
            .sortedBy { it.order }

        dropTablesArchive(dataSource)

        TableMaintenanceDAO.swapTablesAtomically(tablesToSwitch)

        tablesToSwitch.forEach { table ->
            TableMaintenanceDAO.loadScriptAndCreateTable(table)
        }
    }
}
