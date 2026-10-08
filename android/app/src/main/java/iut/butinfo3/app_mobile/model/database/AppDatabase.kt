package iut.butinfo3.app_mobile.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import iut.butinfo3.app_mobile.model.dao.MedicamentDao
import iut.butinfo3.app_mobile.model.dao.OrdonnanceDao
import iut.butinfo3.app_mobile.model.dao.TreatmentDao
import iut.butinfo3.app_mobile.model.dao.UserDao
import iut.butinfo3.app_mobile.model.entity.LinkMedicamentSubstance
import iut.butinfo3.app_mobile.model.entity.LinkUserUser
import iut.butinfo3.app_mobile.model.entity.MedicamentPresDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSpeResume
import iut.butinfo3.app_mobile.model.entity.MedicamentStatusCip
import iut.butinfo3.app_mobile.model.entity.MedicamentStatusCis
import iut.butinfo3.app_mobile.model.entity.MedicamentSubstanceDetail
import iut.butinfo3.app_mobile.model.entity.MedicamentSubstanceResume
import iut.butinfo3.app_mobile.model.entity.TreatmentHistory
import iut.butinfo3.app_mobile.model.entity.TreatmentReminder
import iut.butinfo3.app_mobile.model.entity.User
import iut.butinfo3.app_mobile.model.entity.UserTreatment
import iut.butinfo3.app_mobile.utils.Converters
import iut.butinfo3.app_mobile.model.entity.Ordonnance

/**
 * Initialisation de la db locale créer avec Room.
 *
 * Déclaration de toutes les entités
 *
 * Déclaration des dao
 *
 * L'annotation @Volatile permet que toutes les écritures dans la db soit immédiatement transféré dans
 * la mémoire principale(et non pas dans la mémoire locale d'un thread), toutes les lectures de la db
 * se ont sur la mémoire principale.
 *
 * L'annotation @TypeConverters est obligatoire pour les entités avec des types non-conforme à Room
 * comme les Date par exemple. Par défaut room est très exigeant sur les types autorisé
 * c'est pourquoi il faut définir une classe Converters pour autorisé de nouveau types de données.
 *
 * La fonction getDatabase créer une instance de la db si aucune n'existe à l'origine
 * , sinon elle reprend celle déjà existante.
 */
@Database(entities = [
    User::class,
    LinkUserUser::class,
    LinkMedicamentSubstance::class,
    MedicamentPresDetail::class,
    MedicamentSpeResume::class,
    MedicamentStatusCip::class,
    MedicamentStatusCis::class,
    MedicamentSubstanceDetail::class,
    MedicamentSubstanceResume::class,
    TreatmentReminder::class,
    UserTreatment::class,
    TreatmentHistory::class,
    Ordonnance::class
                     ]
    , version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicamentDao(): MedicamentDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun ordonnanceDao(): OrdonnanceDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE treatment_history ADD COLUMN status TEXT NOT NULL DEFAULT 'TAKEN'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}