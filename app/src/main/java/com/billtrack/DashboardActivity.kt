package com.billtrack

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Transition
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.billtrack.databinding.ActivityDashboardBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.auth.api.signin.GoogleSignIn


class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.hide()

        val googleInfo = GoogleSignIn.getLastSignedInAccount(this)

        if (googleInfo?.photoUrl != null) {
            Glide.with(this)
                .load(googleInfo.photoUrl)
                .circleCrop()
                .into(object : CustomTarget<Drawable?>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in Drawable?>?
                    ) {
                        binding.toolbar.navigationIcon = resource
                    }

                    override fun onLoadCleared(@Nullable placeholder: Drawable?) {
                        // This is called if the target is cleared (e.g., activity destroyed)
                    }
                })
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Setup BottomNavigationView with NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Setup ActionBar with NavController
        // This will automatically update the Toolbar title based on the fragment's label
        // and handle the Up button (though for a flat bottom nav, Up button is less common from the root destinations)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_spending,
                R.id.navigation_goals,
                R.id.navigation_report,
                R.id.navigation_income
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Handle settings icon click in toolbar
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    // Handle settings click
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Handle navigation icon click (user profile placeholder)
        binding.toolbar.setNavigationOnClickListener {
            // Handle navigation icon click (e.g., open profile screen)
            Toast.makeText(this, "User profile icon clicked", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle Up navigation if you navigate deeper and want the Toolbar's Up button to work
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, null) || super.onSupportNavigateUp()
    }
}
