package com.jetbrains.iogallery

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.core.view.children
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.jetbrains.iogallery.support.lastOfType
import com.jetbrains.iogallery.support.setupPicasso
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private val navController
        get() = findNavController(R.id.navHostFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        setupPicasso()
        setHasLightNavigationBar()

        setContentView(R.layout.activity_main)
        toolbar.title = getString(R.string.gallery)
        setSupportActionBar(toolbar)

        NavigationUI.setupActionBarWithNavController(this, navController)

        navController.addOnDestinationChangedListener(this)
    }

    private fun setHasLightNavigationBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        window.decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
            SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                navController.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    fun menuItemViews(): Sequence<ActionMenuItemView> {
        val menuView = toolbar.children.lastOfType(ActionMenuView::class.java)
        return menuView.children.filterIsInstance(ActionMenuItemView::class.java)
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val icon = when(destination.id) {
            R.id.photosGridFragment, R.id.uploadFragment -> null
            else -> resources.getDrawable(R.drawable.ic_arrow_back, toolbar.context.theme)
        }
        toolbar.navigationIcon = icon
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.uploadFragment) {
            return // We ignore back presses while uploading
        }
        super.onBackPressed()
    }
}
