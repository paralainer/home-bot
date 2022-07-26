package com.paralainer.homebot.fastmile

class FastmileService(
    private val fastmileClient: FastmileClient
) {
    suspend fun is5GUp(): Boolean {
        val status = fastmileClient.status()
        return !(status.cell5g.isEmpty() ||
            status.cell5g.first().stat.pci == PCI_WHEN_5G_DOWN)
    }

    suspend fun reboot() = fastmileClient.reboot()

    companion object {
        const val PCI_WHEN_5G_DOWN = 4294967295
    }
}
