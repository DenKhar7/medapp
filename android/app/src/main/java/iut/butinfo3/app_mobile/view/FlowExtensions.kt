package iut.butinfo3.app_mobile.view

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Extension pour collecter un Flow(Flow est un flux asynchrone de donnes) dans un LifecycleOwner (Activity ou Fragment)
 * en respectant son cycle de vie
 * @param state État du cycle de vie dans lequel commencer la collecte
 * @param collector Fonction suspendue pour traiter les éléments collectés dans le Flow 
 */
fun <T> Flow<T>.collectIn(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(state) {
            this@collectIn.collect { collector(it) }
        }
    }
}
