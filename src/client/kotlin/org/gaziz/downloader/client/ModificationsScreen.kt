package org.gaziz.downloader.client

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ModificationsScreen: BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    override fun build(root: FlowLayout) {
        val scroll =
            UIContainers.verticalScroll(
                Sizing.fill(),
                Sizing.fixed(200),
                UIContainers.verticalFlow(
                    Sizing.content(),
                    Sizing.content(),
                ).apply {
                    gap(8)
                    repeat(20,{
                        child(UIComponents.label(Component.literal("$it. Hello")))
                    })
                }
            ).apply {
                scrollbar(ScrollContainer.Scrollbar.vanilla())
                scrollbarThiccness(4)
            }
        root.surface(Surface.blur(0.5f,1f)).alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
        root.child(
            UIContainers.verticalFlow(
                Sizing.content(),
                Sizing.content(),
            )
                .child(UIComponents.textBox(Sizing.fill(),"Hello world!"))
                .child(scroll)
                .child(UIComponents.button(Component.literal("Back"),{
                    Minecraft.getInstance().setScreen(null)
                }))
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10,10,10,10))
        )
    }

}