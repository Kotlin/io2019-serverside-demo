package com.jetbrains.iogallery.list.batch

import java.io.Serializable

enum class BatchOperationType(val requiresConfirmation: Boolean) : Serializable {
    DELETE(requiresConfirmation = true),
    BLACK_AND_WHITE(requiresConfirmation = false);
}
