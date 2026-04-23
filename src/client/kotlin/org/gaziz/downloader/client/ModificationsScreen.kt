package org.gaziz.downloader.client

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.util.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import org.gaziz.downloader.Moddownloadermod

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
                    child(loadingChild)
                    CoroutineScope(Dispatchers.IO).launch {
                        val searchResp = DownloaderClient.search("")
                        for(hit in searchResp.hits) {
                            val texturePath: Observable<String> = Observable.of("textures/default-mod-icon.png")
                            Minecraft.getInstance().execute {
                                child(
                                    ModificationCard(
                                        hit,
                                        project,
                                        texturePath
                                    ) {
                                        project.set(it)
                                        installBtn.onPress {
                                            val installToast = SystemToast(
                                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                                Component.literal("Mod Downloader"),
                                                Component.literal("Downloading ${hit.title}")
                                            )
                                            toastManager.addToast(installToast)
                                            installBtn.active(false)
                                            project.set(null)
                                        }
                                        installBtn.active(true)
                                    }
                                )
                                if(hit == searchResp.hits[0]) {
                                    removeChild(loadingChild)
                                }
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                val name = "[a-z0-9/._-]"
                                    .toRegex()
                                    .findAll(
                                        hit.title
                                            .lowercase()
                                            .replace(" ","-"),
                                        0
                                    )
                                    .joinToString("") { it.value }

                                val texId = Identifier.fromNamespaceAndPath(Moddownloadermod.MOD_ID,name)
                                val nativeImage = DownloaderClient.downloadPhoto(hit.icon_url)

                                Minecraft.getInstance().execute {
                                    val dynTex = DynamicTexture({ name }, nativeImage)

                                    Minecraft.getInstance().textureManager
                                        .register(
                                            texId,
                                            dynTex
                                        )

                                    texturePath.set(name)
                                }
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