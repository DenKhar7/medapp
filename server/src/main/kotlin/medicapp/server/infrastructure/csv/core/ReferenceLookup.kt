package medicapp.server.infrastructure.csv.core

import java.sql.Connection

/**
 * Lectures de référence faites dans la transaction d'import en cours (les lignes déjà insérées mais non
 * validées sont visibles car on utilise la même connexion).
 *
 * Sert à écarter les lignes "orphelines" des fichiers sources : le référentiel RUIM contient des CIP
 * dans certains fichiers (éléments, événements) qui n'existent pas dans les présentations importées,
 * ce qui violerait les clés étrangères de la base.
 */
object ReferenceLookup {

    /** Ensemble des CIP13 présents dans med_presentation. */
    fun presentationCip13s(connection: Connection): Set<String> =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT cip13 FROM med_presentation").use { rs ->
                buildSet {
                    while (rs.next()) add(rs.getString(1))
                }
            }
        }
}
