package com.paralainer.homebot.speedtest

interface SpeedtestService {
    suspend fun measureSpeed(): SpeedtestResult
}

data class SpeedtestResult(val downloadSpeedMbps: Double, val uploadSpeedMbps: Double, val pingMs: Double)
