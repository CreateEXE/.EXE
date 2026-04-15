package com.createexe.exe.core
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object NodeEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _bus = MutableSharedFlow<NodeEvent>(0, 64)
    val bus: SharedFlow<NodeEvent> = _bus.asSharedFlow()
    fun emit(event: NodeEvent) { scope.launch { _bus.emit(event) } }
    fun subscribe(scope: CoroutineScope, filter: (NodeEvent) -> Boolean = { true }, handler: suspend (NodeEvent) -> Unit) {
        scope.launch(Dispatchers.IO) { bus.collect { if (filter(it)) handler(it) } }
    }
}
data class NodeEvent(val type: String, val sourceNode: Int = 0, val payload: Map<String, Any> = emptyMap())
object EventType {
    const val TTS_SPEAK = "TTS_SPEAK"; const val TTS_STOP = "TTS_STOP"
    const val OVERLAY_SHOW = "OVERLAY_SHOW"; const val OVERLAY_HIDE = "OVERLAY_HIDE"
    const val OVERLAY_VRM_LOAD = "OVERLAY_VRM_LOAD"
}
