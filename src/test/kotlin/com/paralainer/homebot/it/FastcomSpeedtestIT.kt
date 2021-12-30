package com.paralainer.homebot.it

import com.paralainer.homebot.speedtest.FastcomSpeedtest
import io.kotest.core.spec.style.WordSpec
import io.kotest.extensions.spring.SpringExtension

@SpringIntegrationTest
class FastcomSpeedtestIT(
    private val fastcomSpeedtest: FastcomSpeedtest
): WordSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        "measureSpeed" should {
            "measure" {
                fastcomSpeedtest.measureSpeed()
            }
        }
    }
}
