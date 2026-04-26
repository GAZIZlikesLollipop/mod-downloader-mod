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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import org.gaziz.modrinthdirect.ModrinthDirect
import java.nio.file.*
import kotlin.io.path.name
import kotlin.time.Duration.Companion.milliseconds

object ModificationsScreen: BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }
    private val toastManager = Minecraft.getInstance().toastManager

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val flow = MutableStateFlow<WatchEvent<*>?>(null)
    init {
        CoroutineScope(Dispatchers.IO).launch {
            DownloaderClient.search("")
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
                    flow.emit(event)
                }
                key.reset()
            }
        }
    }
    override fun build(root: FlowLayout) {
        val project: Observable<String?> = Observable.of(null)
        val searchText:Observable<String> = Observable.of("")
        val installBtn = UIComponents
            .button(
                Component.literal("Install mod"),
                {
                    it.active(false)
                }
            )
            .active(false)

        val intermediateChild = UIContainers
            .verticalFlow(
                Sizing.fill(),
                Sizing.fill(85)
            )
            .child(UIComponents.item(Items.CLOCK.defaultInstance))
            .child(UIComponents.label(Component.literal("Loading...")))
            .gap(6)
            .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)

        val modsList = UIContainers
            .verticalFlow(
                Sizing.content(),
                Sizing.content(),
            )
            .child(intermediateChild)

        CoroutineScope(Dispatchers.IO).launch {
            DownloaderClient
                .searchedMods
                .drop(1)
                .collect { hits ->
                modsList.apply {
                   gap(4)
                    if(hits.isNotEmpty()) {
                       for(hit in hits) {
                           val formattedName = "[a-z0-9/._-]"
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
                                       flow
                                   ) {
                                       project.set(it)
                                       if(isInstalled.get()) {
                                           installBtn.message = Component.literal("Delete mod")
                                       } else {
                                           installBtn.message = Component.literal("Install mod")
                                       }
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
                                                       formattedName
                                                   )
                                                   if (result != null) {
                                                       val noFilesToast = SystemToast(
                                                           SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                           Component.literal("Modrinth Direct"),
                                                           Component.literal("${hit.title} installation error")
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
                                               try {
                                                   Files.delete(Path.of("${Minecraft.getInstance().gameDirectory.path}/mods/${formattedName}.jar"))
                                                   installBtn.message = Component.literal("Install mod")
                                                   val deleteToast = SystemToast(
                                                       SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                       Component.literal("Modrinth Direct"),
                                                       Component.literal("${hit.title} successfully deleted")
                                                   )
                                                   toastManager.addToast(deleteToast)
                                               } catch(_: Exception) {
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
                                   val nativeImage = DownloaderClient.downloadPhoto(hit.icon_url)

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
                                   .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                           )
                       }
                    }
                }
            }
        }

        val searchButton = UIComponents
            .button(
                Component.literal("   "),
                {
                    Minecraft.getInstance().execute {
                        modsList.clearChildren()
                        modsList.child(intermediateChild)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        DownloaderClient.search(searchText.get())
                    }
                }
            )
            .active(false)

        val searchField = UIComponents
            .textBox(Sizing.fill(95), searchText.get())
            .apply {
                setHint(Component.literal("Search mods..."))
                onChanged().subscribe {
                    searchButton.active(it.isNotEmpty())
                    searchText.set(it)
                }
            }

        root.child(
            UIContainers
                .verticalFlow(
                    Sizing.content(),
                    Sizing.content(),
                )
                .child(
                    UIContainers
                        .horizontalFlow(Sizing.content(),Sizing.fill(12))
                        .child(searchField)
                        .child(
                            UIContainers
                                .stack(Sizing.content(), Sizing.content())
                                .child(searchButton)
                                .child(
                                    UIComponents
                                        .item(Items.SPYGLASS.defaultInstance)
                                        .sizing(Sizing.fixed(16))
                                )
                                .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                        )
                        .gap(2)
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .child(
                    UIContainers.verticalScroll(
                        Sizing.fill(),
                        Sizing.fill(85),
                        modsList
                    ).apply {
                        scrollbar(ScrollContainer.Scrollbar.vanilla())
                        scrollbarThiccness(4)
                    }
                )
                .child(
                    UIContainers
                        .horizontalFlow(Sizing.fill(),Sizing.fill(13))
                        .child(
                            UIComponents.button(
                                Component.literal("Back"),
                                { Minecraft.getInstance().setScreen(null) }
                            )
                        )
                        .child(installBtn)
                        .gap(16)
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                )
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10,10,10,10))
        )
        root.surface(Surface.vanillaPanorama(true)).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
    }

}