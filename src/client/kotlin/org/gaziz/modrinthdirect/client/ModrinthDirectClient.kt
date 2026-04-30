package org.gaziz.modrinthdirect.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ModrinthDirectClient : ClientModInitializer {
	override fun onInitializeClient() {
		Path.of("${MinecraftClient.getInstance().runDirectory.path}/mods").createDirectories()
	}
}