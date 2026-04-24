package org.gaziz.downloader.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ModDownloaderClient : ClientModInitializer {
	override fun onInitializeClient() {
		Path.of("${Minecraft.getInstance().gameDirectory.path}/mods").createDirectories()
	}
}