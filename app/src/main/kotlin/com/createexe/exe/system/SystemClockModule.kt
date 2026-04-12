package com.createexe.exe.system
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

object SystemClockModule {
    fun nowIso(): String = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    fun nowHuman(): String = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy hh:mm a"))
    fun timezone(): String = TimeZone.getDefault().id
    fun contextSnapshot(): Map<String, String> = mapOf(
        "current_time_iso" to nowIso(),
        "current_time_human" to nowHuman(),
        "timezone" to timezone()
    )
}
