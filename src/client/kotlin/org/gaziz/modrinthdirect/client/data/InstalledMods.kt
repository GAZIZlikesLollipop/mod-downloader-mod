package org.gaziz.modrinthdirect.client.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.minecraft.client.MinecraftClient
import java.nio.file.Files
import java.nio.file.Path

object InstalledMods {

    private val listDir = Path.of("${MinecraftClient.getInstance().runDirectory.path}/mods/.modrinth-direct")
    private val listFile = Path.of("${listDir}/installed-mods.json")

    private val _installedMods = MutableStateFlow<List<String>>(emptyList())
    val installedMods: StateFlow<List<String>> = _installedMods.asStateFlow()

    init {
        Files.createDirectories(listDir)
        if(Files.exists(listFile)) {
            CoroutineScope(Dispatchers.IO).launch {
                _installedMods.emit(Json.decodeFromString(Files.readString(listFile)))
            }
        } else {
            Files.createFile(listFile)
        }
    }

    private fun writeToFile(list: List<String>){
       Files.writeString(listFile,Json.encodeToString(list))
    }

    suspend fun addMod(slug: String) {
        val list = installedMods.value.toMutableList().apply {add(slug)}.toList()
        _installedMods.emit(list)
        writeToFile(list)
    }

    suspend fun removeMod(slug: String) {
        val list = installedMods.value - slug
        _installedMods.emit(list)
        writeToFile(list)
    }

}