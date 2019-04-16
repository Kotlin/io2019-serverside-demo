package com.jetbrains.iogallery.debug

import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.jetbrains.iogallery.R

fun Fragment.onDebugOptionsItemSelected(item: MenuItem, onServerChangedListener: () -> Unit): Boolean {
    if (item.itemId == R.id.menu_debug_server_select) {
        val dialogFragment = ServerSelectDialogFragment()
        dialogFragment.onServerChangedListener = onServerChangedListener
        dialogFragment.showNow(requireActivity().supportFragmentManager, "SERVER_SELECT")
        return true
    }
    return false
}

