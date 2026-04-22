package org.gaziz.downloader.client

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ModificationsScreen: BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    override fun build(root: FlowLayout) {
        var project: String? = null
        val scroll =
            UIContainers.verticalScroll(
                Sizing.fill(),
                Sizing.fixed(200),
                UIContainers.verticalFlow(
                    Sizing.content(),
                    Sizing.content(),
                ).apply {
                    gap(8)
                    val gameVersion = FabricLoader.getInstance().rawGameVersion
//                    CoroutineScope(Dispatchers.IO).launch {
//                        val searchResp = DownloaderClient.search("")
//                        for(hit in searchResp.hits) {
//                            if(hit.versions.find{it == gameVersion} != null) {
//                                CoroutineScope(Dispatchers.Main).launch {
//                                    child(
//                                        ModificationCard(hit) {
//                                                s -> project = s
//                                        }
//                                    )
//                                }
//                            }
//                        }
//                    }
                    runBlocking {
                        val searchResp = DownloaderClient.search("")
                        for(hit in searchResp.hits) {
                            if(hit.versions.find{it == gameVersion} != null) {
                                child(
                                    ModificationCard(hit) {
                                            s -> project = s
                                    }
                                )
                            }
                        }
                    }
                }
            ).apply {
                scrollbar(ScrollContainer.Scrollbar.vanilla())
                scrollbarThiccness(4)
            }
        root.surface(Surface.blur(1f,0.5f)).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
        root.child(
            UIContainers.verticalFlow(
                Sizing.content(),
                Sizing.content(),
            )
                .child(UIComponents.textBox(Sizing.fill(),"Hello world!"))
                .child(scroll)
                .child(
                    UIContainers
                        .horizontalFlow(Sizing.content(),Sizing.content())
                        .child(
                            UIComponents.button(
                                Component.literal("Back"),
                                { Minecraft.getInstance().setScreen(null) }
                            )
                        )
                        .child(
                            UIComponents.button(
                                Component.literal("Install mod"),
                                {  }
                            )
                        )
                        .gap(16)
                )
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10,10,10,10))
        )
    }

}