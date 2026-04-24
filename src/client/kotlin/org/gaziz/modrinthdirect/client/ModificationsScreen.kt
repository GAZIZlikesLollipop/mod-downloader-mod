package org.gaziz.modrinthdirect.client

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.util.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import org.gaziz.modrinthdirect.ModrinthDirect
import org.slf4j.LoggerFactory
import java.nio.file.*
import kotlin.io.path.name
import kotlin.time.Duration.Companion.milliseconds

object ModificationsScreen: BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }
    private val toastManager = Minecraft.getInstance().toastManager

    override fun build(root: FlowLayout) {
        val project: Observable<String?> = Observable.of(null)
        val fieldText = "Hello world!"
        val installBtn = UIComponents
            .button(
                Component.literal("Install mod"),
                {
                    it.active(false)
                }
            )
            .active(false)
        val scroll =
            UIContainers.verticalScroll(
                Sizing.fill(),
                Sizing.fill(85),
                UIContainers.verticalFlow(
                    Sizing.content(),
                    Sizing.content(),
                ).apply {
                    gap(4)
                    val loadingChild = UIContainers
                        .verticalFlow(
                            Sizing.fill(),
                            Sizing.fill(85)
                        )
                        .child(UIComponents.item(Items.CLOCK.defaultInstance))
                        .child(UIComponents.label(Component.literal("Loading...")))
                        .gap(6)
                        .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                    val watchService = FileSystems.getDefault().newWatchService()
                    val path = Paths.get("${Minecraft.getInstance().gameDirectory.path}/mods")
                    path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                    )
                    child(loadingChild)
                    CoroutineScope(Dispatchers.IO).launch {
                        val searchResp = DownloaderClient.search("")
                        if(searchResp.hits.isNotEmpty()) {
                            for(hit in searchResp.hits) {
                                var fileName = "[a-z0-9/._-]"
                                    .toRegex()
                                    .findAll(
                                        hit.title
                                            .lowercase()
                                            .replace(" ","-"),
                                        0
                                    )
                                    .joinToString("") { it.value }

                                val texturePath: Observable<String> = Observable.of("textures/default-mod-icon.png")
                                val isInstalled: Observable<Boolean> = Observable.of(false)
                                Minecraft.getInstance().execute {
                                    if(hit == searchResp.hits[0]) {
                                        removeChild(loadingChild)
                                    }
                                    val modsDir = Path.of("${Minecraft.getInstance().gameDirectory.path}/mods")
                                    Files.list(modsDir).use { files ->
                                        files.forEach { f ->
                                            if("$fileName.jar" == f.name) {
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
                                        ) {
                                            project.set(it)
                                            installBtn.onPress {
                                                installBtn.active(false)
                                                project.set(null)
                                                if(!isInstalled.get()){
                                                    val installToast = SystemToast(
                                                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                        Component.literal("Modrinth Direct"),
                                                        Component.literal("Downloading ${hit.title}")
                                                    )
                                                    toastManager.addToast(installToast)
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        val result = DownloaderClient.downloadMod(
                                                            hit.project_id,
                                                            fileName
                                                        )
                                                        if (result != null) {
                                                            val noFilesToast = SystemToast(
                                                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                                Component.literal("Modrinth Direct"),
                                                                Component.literal(result.message ?: "Download Error")
                                                            )
                                                            toastManager.addToast(noFilesToast)
                                                        } else {
                                                            val installedToast = SystemToast(
                                                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                                Component.literal("Modrinth Direct"),
                                                                Component.literal("${hit.title} successfully installed")
                                                            )
                                                            val alertToast = SystemToast(
                                                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                                Component.literal("Modrinth Direct"),
                                                                Component.literal("Restart the game to enable the mod")
                                                            )
                                                            toastManager.addToast(installedToast)
                                                            delay(2000.milliseconds)
                                                            toastManager.addToast(alertToast)
                                                        }
                                                    }
                                                } else {
                                                    val installedToast = SystemToast(
                                                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                        Component.literal("Modrinth Direct"),
                                                        Component.literal("Mod is already installed")
                                                    )
                                                    toastManager.addToast(installedToast)
                                                }
                                            }
                                            installBtn.active(true)
                                        }
                                    )
                                    CoroutineScope(Dispatchers.IO).launch {
                                        while(true) {
                                            val key = watchService.take()
                                            for(event in key.pollEvents()) {
                                                val newEvent = event as WatchEvent<Path>
                                                val logger = LoggerFactory.getLogger(ModrinthDirect.MOD_ID)
                                                logger.info("$fileName ${newEvent.context().name}")
                                                if("$fileName.jar" == newEvent.context().name) {
                                                    if(event.kind() === StandardWatchEventKinds.ENTRY_CREATE) {
                                                        isInstalled.set(true)
                                                    } else {
                                                        isInstalled.set(false)
                                                    }
//                                                    break
                                                }
                                            }
                                        }
                                    }

                                    CoroutineScope(Dispatchers.IO).launch {
                                        fileName = "[a-z0-9/._-]"
                                            .toRegex()
                                            .findAll(
                                                hit.title
                                                    .lowercase()
                                                    .replace(" ","-"),
                                                0
                                            )
                                            .joinToString("") { it.value }
                                        val texId = Identifier.fromNamespaceAndPath(ModrinthDirect.MOD_ID,fileName)
                                        val nativeImage = DownloaderClient.downloadPhoto(hit.icon_url)

                                        Minecraft.getInstance().execute {
                                            val dynTex = DynamicTexture({ fileName }, nativeImage)

                                            Minecraft.getInstance().textureManager
                                                .register(
                                                    texId,
                                                    dynTex
                                                )

                                            texturePath.set(fileName)
                                        }
                                    }
                                }

                            }
                        } else {
                            Minecraft.getInstance().execute {
                                removeChild(loadingChild)
                                child(
                                    0,
                                    UIContainers
                                        .verticalFlow(
                                            Sizing.fill(),
                                            Sizing.fill(85)
                                        )
                                        .child(UIComponents.item(Items.BARRIER.defaultInstance))
                                        .child(UIComponents.label(Component.literal("No results found")).color(Color.RED))
                                        .gap(6)
                                        .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                                )
                            }
                        }
                    }
                }
            ).apply {
                scrollbar(ScrollContainer.Scrollbar.vanilla())
                scrollbarThiccness(4)
            }
        root.child(
            UIContainers.verticalFlow(
                Sizing.content(),
                Sizing.content(),
            )
                .child(
                    UIContainers
                        .verticalFlow(Sizing.content(),Sizing.fill(12))
                        .child(UIComponents.textBox(Sizing.fill(),fieldText))
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .child(scroll)
                .child(
                    UIContainers
                        .horizontalFlow(Sizing.content(),Sizing.fill(13))
                        .child(
                            UIComponents.button(
                                Component.literal("Back"),
                                { Minecraft.getInstance().setScreen(null) }
                            )
                        )
                        .child(installBtn)
                        .gap(16)
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10,10,10,10))
        )
        root.surface(Surface.vanillaPanorama(true)).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
    }

}