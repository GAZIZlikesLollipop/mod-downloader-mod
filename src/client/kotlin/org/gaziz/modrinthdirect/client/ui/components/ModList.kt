package org.gaziz.modrinthdirect.client.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.util.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import org.gaziz.modrinthdirect.ModrinthDirect
import org.gaziz.modrinthdirect.client.api.ApiClient
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen.formatTitle
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen.intermediateChild
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen.modsDirFlow
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

class ModList(
    installBtn: ButtonComponent
): FlowLayout(
    Sizing.fill(),
    Sizing.fill(85),
    Algorithm.VERTICAL
) {
    private val toastManager = Minecraft.getInstance().toastManager

    val list: FlowLayout = UIContainers
        .verticalFlow(
            Sizing.content(),
            Sizing.content(),
        )
        .child(intermediateChild)

    private val project: Observable<String?> = Observable.of(null)

    init {
        this.child(
            UIContainers.verticalScroll(
                Sizing.fill(),
                Sizing.fill(),
                list
            ).apply {
                scrollbar(ScrollContainer.Scrollbar.vanilla())
                scrollbarThiccness(4)
            }
        )
        CoroutineScope(Dispatchers.IO).launch {
            ApiClient
                .searchedMods
                .drop(1)
                .collect { hits ->
                    list.apply {
                        gap(4)
                        if(hits.isNotEmpty()) {
                            for(hit in hits) {
                                val formattedName = formatTitle(hit.slug)

                                val texturePath: Observable<String> = Observable.of("textures/default-mod-icon.png")
                                val isInstalled: Observable<Boolean> = Observable.of(false)
                                Minecraft.getInstance().execute {
                                    if(hit == hits[0]) {
                                        clearChildren()
                                    }
                                    val modsDir = Path.of("${Minecraft.getInstance().gameDirectory.path}/mods")
                                    Files.list(modsDir).use { files ->
                                        files.forEach { f ->
                                            if("$formattedName.jar" == f.name) {
                                                isInstalled.set(true)
                                            }
                                        }
                                    }
                                    child(
                                        ModificationCard(
                                            hit,
                                            project,
                                            texturePath,
                                            isInstalled,
                                            modsDirFlow
                                        ) {
                                            if (isInstalled.get()) {
                                                installBtn.message = Component.literal("Delete mod")
                                            } else {
                                                installBtn.message = Component.literal("Install mod")
                                            }
                                            installBtn.onPress {
                                                installBtn.active(false)
                                                project.set(null)
                                                if (!isInstalled.get()) {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        ApiClient.startDownload(hit.slug)
                                                    }
                                                } else {
                                                    try {
                                                        Files.delete(Path.of("${Minecraft.getInstance().gameDirectory.path}/mods/${formattedName}.jar"))
                                                        installBtn.message = Component.literal("Install mod")
                                                        val deleteToast = SystemToast(
                                                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                            Component.literal("Modrinth Direct"),
                                                            Component.literal("${hit.title} successfully deleted")
                                                        )
                                                        toastManager.addToast(deleteToast)
                                                    } catch (_: Exception) {
                                                        val deleteToast = SystemToast(
                                                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                            Component.literal("Modrinth Direct"),
                                                            Component.literal("Error of deleting ${hit.title}")
                                                        )
                                                        toastManager.addToast(deleteToast)
                                                    }
                                                }
                                            }
                                            installBtn.active(true)
                                        }
                                    )
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val texId = Identifier.fromNamespaceAndPath(ModrinthDirect.MOD_ID,formattedName)
                                        val nativeImage = ApiClient.downloadPhoto(hit.iconUrl)

                                        Minecraft.getInstance().execute {
                                            val dynTex = DynamicTexture({ formattedName }, nativeImage)

                                            Minecraft.getInstance().textureManager
                                                .register(
                                                    texId,
                                                    dynTex
                                                )

                                            texturePath.set(formattedName)
                                        }
                                    }
                                }
                            }
                        } else {
                            Minecraft.getInstance().execute {
                                clearChildren()
                                child(
                                    UIContainers
                                        .verticalFlow(
                                            Sizing.fill(),
                                            Sizing.fill(85)
                                        )
                                        .child(UIComponents.item(Items.BARRIER.defaultInstance))
                                        .child(UIComponents.label(Component.literal("No results found")).color(Color.RED))
                                        .gap(6)
                                        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                                )
                            }
                        }
                    }
                }
        }
    }
}