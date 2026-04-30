package org.gaziz.modrinthdirect.client.ui.state

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.item.Items
import net.minecraft.text.Text
import org.gaziz.modrinthdirect.client.api.ApiClient
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService

object StateHelper {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.search("")
            val watchService: WatchService = FileSystems.getDefault().newWatchService()
            Paths.get("${MinecraftClient.getInstance().runDirectory.path}/mods").apply {
                register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                )
            }
            while(true) {
                val key = watchService.take()
                for(event in key.pollEvents()) {
                    modsDirFlow.emit(event)
                }
                key.reset()
            }
        }
    }

    fun formatTitle(title: String) = "[a-z0-9/._-]"
        .toRegex()
        .findAll(
            title
                .lowercase()
                .replace(" ","-"),
            0
        )
        .joinToString("") { it.value }

    val intermediateChild: ParentUIComponent = UIContainers
        .verticalFlow(
            Sizing.fill(),
            Sizing.fill(85)
        )
        .child(UIComponents.item(Items.CLOCK.defaultStack))
        .child(UIComponents.label(Text.literal("Loading...")))
        .gap(6)
        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

    val modsDirFlow = MutableStateFlow<WatchEvent<*>?>(null)

}