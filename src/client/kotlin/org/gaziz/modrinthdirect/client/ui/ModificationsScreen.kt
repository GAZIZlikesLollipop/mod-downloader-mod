package org.gaziz.modrinthdirect.client.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import org.gaziz.modrinthdirect.client.api.ApiClient
import org.gaziz.modrinthdirect.client.api.models.DownloadState
import org.gaziz.modrinthdirect.client.ui.components.BottomRow
import org.gaziz.modrinthdirect.client.ui.components.ModList
import org.gaziz.modrinthdirect.client.ui.components.SearchRow
import java.nio.file.*

object ModificationsScreen: BaseOwoScreen<FlowLayout>() {
    fun formatTitle(title: String) = "[a-z0-9/._-]"
        .toRegex()
        .findAll(
            title
                .lowercase()
                .replace(" ","-"),
            0
        )
        .joinToString("") { it.value }

    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    private val toastManager = Minecraft.getInstance().toastManager

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    val modsDirFlow = MutableStateFlow<WatchEvent<*>?>(null)

    val intermediateChild: ParentUIComponent = UIContainers
        .verticalFlow(
            Sizing.fill(),
            Sizing.fill(85)
        )
        .child(UIComponents.item(Items.CLOCK.defaultInstance))
        .child(UIComponents.label(Component.literal("Loading...")))
        .gap(6)
        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.search("")
            Paths.get("${Minecraft.getInstance().gameDirectory.path}/mods").apply {
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

    override fun build(root: FlowLayout) {
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.downloadState.collect {
                it.toList().forEach { m ->
                    when(m.second) {
                        is DownloadState.Error -> {
                            val noFilesToast = SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.literal("Modrinth Direct"),
                                Component.literal("${m.first} installation error")
                            )
                            toastManager.addToast(noFilesToast)
                        }
                        is DownloadState.Loading -> {
                            val installToast = SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.literal("Modrinth Direct"),
                                Component.literal("Downloading ${m.first}")
                            )
                            toastManager.addToast(installToast)
                        }
                        is DownloadState.OK -> {
                            ApiClient.removeDownloadState(m.first)
                            val installedToast = SystemToast(
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.literal("Modrinth Direct"),
                                Component.literal("${m.first} successfully installed, restart to enable mod")
                            )
                            toastManager.addToast(installedToast)
                        }
                    }
                }
            }
        }

        val bottomRow = BottomRow()
        val modList = ModList(bottomRow.installBtn)
        val searchRow = SearchRow(
            bottomRow.installBtn,
            modList.list
        )

        root.child(
            UIContainers
                .verticalFlow(
                    Sizing.content(),
                    Sizing.content(),
                )
                .child(searchRow)
                .child(modList)
                .child(bottomRow)
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10))
        )

        root
            .surface(Surface.vanillaPanorama(true))
            .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
    }

}