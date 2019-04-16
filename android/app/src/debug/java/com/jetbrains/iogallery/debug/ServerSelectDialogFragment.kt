package com.jetbrains.iogallery.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jetbrains.iogallery.R
import com.jetbrains.iogallery.api.ApiServer

class ServerSelectDialogFragment : BottomSheetDialogFragment() {

    var onServerChangedListener: (() -> Unit)? = null

    private lateinit var debugPreferences: DebugPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_select_server, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugPreferences = DebugPreferences(requireActivity())
        debugPreferences.subscribeToApiServer(this, ::onSelectedServerChanged)

        view.findViewById<Button>(R.id.okButton).setOnClickListener { onSelectionConfirmed() }
        view.findViewById<Button>(R.id.cancelButton).setOnClickListener { dismiss() }
    }

    private val swaggerRadio
        get() = requireView().findViewById<RadioButton>(R.id.swaggerRadio)

    private val gcpRadio
        get() = requireView().findViewById<RadioButton>(R.id.gcpRadio)

    private fun onSelectedServerChanged(apiServer: ApiServer) {
        when (apiServer) {
            ApiServer.SWAGGER -> {
                swaggerRadio.isChecked = true
                gcpRadio.isChecked = false
            }
            ApiServer.GCP -> {
                swaggerRadio.isChecked = false
                gcpRadio.isChecked = true
            }
        }
    }

    private fun onSelectionConfirmed() {
        val apiServer = if (swaggerRadio.isChecked) ApiServer.SWAGGER else ApiServer.GCP
        debugPreferences.apiServer = apiServer
        dismiss()
        onServerChangedListener?.invoke()
    }
}
