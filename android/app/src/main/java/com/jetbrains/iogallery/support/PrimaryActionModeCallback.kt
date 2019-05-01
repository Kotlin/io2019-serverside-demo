package com.jetbrains.iogallery.support

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IntRange
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentActivity

class PrimaryActionModeCallback : ActionMode.Callback {

    var onActionItemClickListener: ((item: MenuItem) -> Unit)? = null
    var onActionModeFinishedListener: (() -> Unit)? = null

    private var actionMode: ActionMode? = null

    @MenuRes
    private var menuResId: Int = 0

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        this.actionMode = mode
        mode.menuInflater.inflate(menuResId, menu)
        mode.title = selectedItemsCount.toString()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        onActionModeFinishedListener?.invoke()
        this.actionMode = null
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        onActionItemClickListener?.invoke(item)
        mode.finish()
        return true
    }

    fun startActionMode(activity: FragmentActivity, @MenuRes menuResId: Int) {
        this.menuResId = menuResId
        selectedItemsCount = 1
        (activity as AppCompatActivity).startSupportActionMode(this)
    }

    fun finishActionMode() {
        actionMode?.finish()
    }

    @IntRange(from = 0L)
    var selectedItemsCount: Int = 1
        set(value) {
            field = value
            actionMode?.title = value.toString()
        }

    val isActive: Boolean
        get() = actionMode != null
}
