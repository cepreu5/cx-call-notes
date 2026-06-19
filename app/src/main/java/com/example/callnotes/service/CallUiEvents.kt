package com.example.callnotes.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class KnownCallEvent(val phone: String, val name: String, val note: String?)
data class UnknownCallEvent(val phone: String)

object CallUiEvents {
    private val _known = MutableSharedFlow<KnownCallEvent>(extraBufferCapacity = 1)
    private val _unknown = MutableSharedFlow<UnknownCallEvent>(extraBufferCapacity = 1)
    val known: SharedFlow<KnownCallEvent> = _known
    val unknown: SharedFlow<UnknownCallEvent> = _unknown
    fun emitKnown(phone: String, name: String, note: String?) {
        _known.tryEmit(KnownCallEvent(phone, name, note))
    }
    fun emitUnknown(phone: String) {
        _unknown.tryEmit(UnknownCallEvent(phone))
    }
}
