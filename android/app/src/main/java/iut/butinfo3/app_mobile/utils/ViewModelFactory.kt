package iut.butinfo3.app_mobile.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import iut.butinfo3.app_mobile.reminder.ReminderManager
import iut.butinfo3.app_mobile.model.database.AppDatabase
import iut.butinfo3.app_mobile.model.repository.AuthRepository
import iut.butinfo3.app_mobile.model.api.MedicamentApiClient
import iut.butinfo3.app_mobile.model.repository.MedicamentRepository
import iut.butinfo3.app_mobile.model.repository.OrdonnanceRepository
import iut.butinfo3.app_mobile.model.repository.SessionRepository
import iut.butinfo3.app_mobile.model.repository.TreatmentRepository
import iut.butinfo3.app_mobile.model.repository.UserRepository
import iut.butinfo3.app_mobile.view_model.AccountViewModel
import iut.butinfo3.app_mobile.view_model.AddProfileViewModel
import iut.butinfo3.app_mobile.view_model.AddTreatmentViewModel
import iut.butinfo3.app_mobile.view_model.ConnectionViewModel
import iut.butinfo3.app_mobile.view_model.EditProfileViewModel
import iut.butinfo3.app_mobile.view_model.HomeViewModel
import iut.butinfo3.app_mobile.view_model.MainViewModel
import iut.butinfo3.app_mobile.view_model.MedicamentViewModel
import iut.butinfo3.app_mobile.view_model.OrdonnanceViewModel
import iut.butinfo3.app_mobile.view_model.RegisterViewModel
import iut.butinfo3.app_mobile.view_model.SplashViewModel
import iut.butinfo3.app_mobile.view_model.UserTreatmentViewModel

/**
 * Par défaut, le système Android ne sait instancier que des ViewModels sans paramètres.
 * Dès qu'un ViewModel a un constructeur Android ne sait plus faire.
 *
 * Cette interface `ViewModelProvider.Factory` permet de :
 * 1. Intercepter la demande de création du ViewModel.
 * 2. Préparer les dépendances nécessaires (les Repositories, la db...).
 * 3. Fabriquer nous-mêmes l'instance du ViewModel avec ses ingrédients.
 * 4. La rendre au système Android.
 *
 * C'est le pont obligatoire pour faire de l'Injection de Dépendances manuelle.
 *
 * Cette Factory sert d'intermédiaire :
 * 1. L'Activité demande un ViewModel.
 * 2. Android demande à la Factory : "Peux-tu me fabriquer ce ViewModel ?"
 * 3. La Factory initialise les Repositories nécessaires.
 * 4. La Factory instancie le ViewModel avec les bons ingrédients.
 *
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    private val db = AppDatabase.getDatabase(context)

    /**
     * <T : ViewModel> -> Déclaration de type générique, on ne connait pas T,
     * il faut donc le présenter avant d'appeler les arguments de la fonction.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val userDao = db.userDao()
        val treatmentDao = db.treatmentDao()
        val medicamentDao = db.medicamentDao()
        val ordonnanceDao = db.ordonnanceDao()

        val userRepo = UserRepository(userDao)
        val authRepo = AuthRepository(context, userDao)
        val treatmentRepo = TreatmentRepository(treatmentDao)
        val ordoRepo = OrdonnanceRepository(ordonnanceDao)
        val medicamentApiClient = MedicamentApiClient()
        val medicamentRepo = MedicamentRepository(medicamentDao, medicamentApiClient)

        when{
            modelClass.isAssignableFrom(SplashViewModel::class.java) ->
                return SplashViewModel(authRepo, userRepo) as T

            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                return MainViewModel(userRepo, authRepo, SessionRepository) as T

            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                return HomeViewModel(treatmentRepo, SessionRepository) as T

            modelClass.isAssignableFrom(AccountViewModel::class.java) ->
                return AccountViewModel(authRepo, userRepo) as T

            modelClass.isAssignableFrom(AddProfileViewModel::class.java) ->{
                val currentUserId = authRepo.getActiveUserId() ?: -1
                return AddProfileViewModel(userRepo, currentUserId) as T
            }
            modelClass.isAssignableFrom(AddTreatmentViewModel::class.java) -> {
                val currentUserId = authRepo.getActiveUserId() ?: -1
                val reminderManager = ReminderManager(context)
                return AddTreatmentViewModel(treatmentRepo,userRepo,currentUserId, reminderManager) as T
            }
            modelClass.isAssignableFrom(ConnectionViewModel::class.java) ->
                return ConnectionViewModel( authRepo) as T

            modelClass.isAssignableFrom(EditProfileViewModel::class.java) ->
                return EditProfileViewModel(userRepo) as T

            modelClass.isAssignableFrom(MedicamentViewModel::class.java) ->
                return MedicamentViewModel(medicamentRepo) as T

            modelClass.isAssignableFrom(OrdonnanceViewModel::class.java) ->
                return OrdonnanceViewModel(ordoRepo, authRepo) as T

            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                return RegisterViewModel( authRepo) as T

            modelClass.isAssignableFrom(UserTreatmentViewModel::class.java) ->
                return UserTreatmentViewModel(treatmentRepo, SessionRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }


    }
}