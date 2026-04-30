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
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.toast.SystemToast
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.gaziz.modrinthdirect.ModrinthDirect
import org.gaziz.modrinthdirect.client.api.ApiClient
import org.gaziz.modrinthdirect.client.ui.state.StateHelper
import org.gaziz.modrinthdirect.client.ui.state.StateHelper.formatTitle
import org.gaziz.modrinthdirect.client.ui.state.StateHelper.intermediateChild
import org.gaziz.modrinthdirect.client.ui.state.StateHelper.modsDirFlow
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
    private val toastManager = MinecraftClient.getInstance().toastManager

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
                .collect { hits ->
                    if(hits != null) {
                        list.apply {
                            gap(4)
                            if (hits.isNotEmpty()) {
                                for (hit in hits) {
                                    val formattedName = formatTitle(hit.slug)

                                    val isInstalled: Observable<Boolean> = Observable.of(false)
                                    MinecraftClient.getInstance().execute {
                                        if (hit == hits[0]) {
                                            clearChildren()
                                        }
                                        val modsDir = Path.of("${MinecraftClient.getInstance().runDirectory.path}/mods")
                                        Files.list(modsDir).use { files ->
                                            files.forEach { f ->
                                                if ("$formattedName.jar" == f.name) {
                                                    isInstalled.set(true)
                                                }
                                            }
                                        }
                                        child(
                                            ModificationCard(
                                                hit,
                                                project,
                                                isInstalled,
                                                modsDirFlow
                                            ) {
                                                if (isInstalled.get()) {
                                                    installBtn.message = Text.literal("Delete mod")
                                                } else {
                                                    installBtn.message = Text.literal("Install mod")
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
                                                            Files.delete(Path.of("${MinecraftClient.getInstance().runDirectory.path}/mods/${formattedName}.jar"))
                                                            installBtn.message = Text.literal("Install mod")
                                                            val deleteToast = SystemToast(
                                                                SystemToast.Type.PERIODIC_NOTIFICATION,
                                                                Text.literal("Modrinth Direct"),
                                                                Text.literal("${hit.title} successfully deleted")
                                                            )
                                                            toastManager.add(deleteToast)
                                                        } catch (_: Exception) {
                                                            val deleteToast = SystemToast(
                                                                SystemToast.Type.PERIODIC_NOTIFICATION,
                                                                Text.literal("Modrinth Direct"),
                                                                Text.literal("Error of deleting ${hit.title}")
                                                            )
                                                            toastManager.add(deleteToast)
                                                        }
                                                    }
                                                }
                                                installBtn.active(true)
                                            }
                                        )
                                        if(StateHelper.cachedIcons.value[hit.slug] == null) {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val texId = Identifier.of(ModrinthDirect.MOD_ID, formattedName)
                                                val nativeImage = ApiClient.downloadPhoto(hit.iconUrl)

                                                MinecraftClient.getInstance().execute {
                                                    val texture = NativeImageBackedTexture({ "" }, nativeImage)

                                                    MinecraftClient.getInstance().textureManager.registerTexture(
                                                        texId,
                                                        texture
                                                    )
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        StateHelper.cachedIcons.emit(StateHelper.cachedIcons.value.toMutableMap().apply { set(hit.slug,formattedName) }.toMap())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                MinecraftClient.getInstance().execute {
                                    clearChildren()
                                    child(
                                        UIContainers
                                            .verticalFlow(
                                                Sizing.fill(),
                                                Sizing.fill(85)
                                            )
                                            .child(UIComponents.item(Items.BARRIER.defaultStack))
                                            .child(
                                                UIComponents.label(Text.literal("No results found")).color(Color.RED)
                                            )
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
}