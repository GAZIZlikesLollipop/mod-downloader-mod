package org.gaziz.modrinthdirect.client.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text
import org.gaziz.modrinthdirect.client.api.ApiClient
import org.gaziz.modrinthdirect.client.api.models.DownloadState
import org.gaziz.modrinthdirect.client.ui.components.BottomRow
import org.gaziz.modrinthdirect.client.ui.components.ModList
import org.gaziz.modrinthdirect.client.ui.components.SearchRow

class ModificationsScreen(val previous: Screen): BaseOwoScreen<FlowLayout>() {

    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    private val toastManager = MinecraftClient.getInstance().toastManager

    override fun build(root: FlowLayout) {
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient.downloadState.collect {
                it.toList().forEach { m ->
                    when(m.second) {
                        is DownloadState.Error -> {
                            val noFilesToast = SystemToast(
                                SystemToast.Type.PERIODIC_NOTIFICATION,
                                Text.literal("Modrinth Direct"),
                                Text.literal("${m.first} installation error")
                            )
                            toastManager.add(noFilesToast)
                        }
                        is DownloadState.Loading -> {
                            val installToast = SystemToast(
                                SystemToast.Type.PERIODIC_NOTIFICATION,
                                Text.literal("Modrinth Direct"),
                                Text.literal("Downloading ${m.first}")
                            )
                            toastManager.add(installToast)
                        }
                        is DownloadState.OK -> {
                            ApiClient.removeDownloadState(m.first)
                            val installedToast = SystemToast(
                                SystemToast.Type.PERIODIC_NOTIFICATION,
                                Text.literal("Modrinth Direct"),
                                Text.literal("${m.first} successfully installed, restart to enable mod")
                            )
                            toastManager.add(installedToast)
                        }
                    }
                }
            }
        }

        val bottomRow = BottomRow(previous,this)
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