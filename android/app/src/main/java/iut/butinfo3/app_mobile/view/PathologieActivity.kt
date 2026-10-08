package iut.butinfo3.app_mobile.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import iut.butinfo3.app_mobile.databinding.PathologieActivityBinding
import iut.butinfo3.app_mobile.utils.NavigationHelper

class PathologieActivity : AppCompatActivity() {
    private lateinit var navigationHelper: NavigationHelper

    private lateinit var binding: PathologieActivityBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PathologieActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        navigationHelper = NavigationHelper(
            binding.navHome,
            binding.navUserTreatments,
            binding.navMedicaments,
            binding.navOrdonnances,
            binding.navPathologie
        )
        navigationHelper.init(NavigationHelper.NAV_HOME)

        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        binding.navUserTreatments.setOnClickListener {
            startActivity(Intent(this, UserTreatmentsActivity::class.java))
        }

        binding.navMedicaments.setOnClickListener {
            startActivity(Intent(this, MedicamentActivity::class.java))
        }

        binding.navPathologie.setOnClickListener {
        }

        binding.navOrdonnances.setOnClickListener {
            startActivity(Intent(this, OrdonnanceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        navigationHelper.setActiveTab(NavigationHelper.NAV_PATHOLOGIE)
    }
}