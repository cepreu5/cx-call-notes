package com.example.callnotes.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class KnownCallEvent(val phone: String, val name: String, val note: String?, val sessionId: Long)
data class UnknownCallEvent(val phone: String, val sessionId: Long)

object CallUiEvents {
    private val _known = MutableSharedFlow<KnownCallEvent>(extraBufferCapacity = 1)
    private val _unknown = MutableSharedFlow<UnknownCallEvent>(extraBufferCapacity = 1)
    val known: SharedFlow<KnownCallEvent> = _known
    val unknown: SharedFlow<UnknownCallEvent> = _unknown
    fun emitKnown(phone: String, name: String, note: String?, sessionId: Long) {
        _known.tryEmit(KnownCallEvent(phone, name, note, sessionId))
    }
    fun emitUnknown(phone: String, sessionId: Long) {
        _unknown.tryEmit(UnknownCallEvent(phone, sessionId))
    }
}
