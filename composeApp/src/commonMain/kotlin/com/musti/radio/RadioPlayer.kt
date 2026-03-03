package com.musti.radio

interface RadioPlayer {
    fun play(url: String, fallbackUrls: List<String> = emptyList())
    fun stop()
    fun setVolume(volume: Float)
    fun setStatusListener(listener: (String) -> Unit)
    fun diagnosticsSummary(): String
    fun setSleepTimer(minutes: Int)
    fun cancelSleepTimer()
    fun sleepTimerRemainingMinutes(): Int
}
