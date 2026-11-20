package id.algaffar.jwstv.data.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class LogLevel(val id: String, val label: String) {
    INFO("info", "INFO"),
    WARN("warn", "WARN"),
    ERROR("error", "ERROR"),
    DEBUG("debug", "DEBUG")
}

data class AppLogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

/**
 * In-memory real-time activity logger for the JWS TV System Log Viewer.
 * Mirrors `src/utils/logger.ts` and `src/components/Settings/LogViewer.tsx`.
 */
object AppLogger {
    private const val MAX_ENTRIES = 300
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val lock = Any()
    private var nextId = 1L

    private val _logsFlow = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<AppLogEntry>> = _logsFlow.asStateFlow()

    fun info(tag: String, message: String) = append(LogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = append(LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = append(LogLevel.ERROR, tag, message)
    fun debug(tag: String, message: String) = append(LogLevel.DEBUG, tag, message)

    fun append(level: LogLevel, tag: String, message: String) {
        synchronized(lock) {
            val entry = AppLogEntry(
                id = nextId++,
                timestamp = ZonedDateTime.now().format(timeFormatter),
                level = level,
                tag = tag,
                message = message
            )
            val updated = (_logsFlow.value + entry).takeLast(MAX_ENTRIES)
            _logsFlow.value = updated
        }
    }

    fun clear() {
        synchronized(lock) {
            _logsFlow.value = emptyList()
        }
    }
}
