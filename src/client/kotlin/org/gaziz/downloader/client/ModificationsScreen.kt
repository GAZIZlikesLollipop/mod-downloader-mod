package org.gaziz.downloader.client

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.ChatFormatting
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
                    child(
                        ModificationCard(
                            SearchHit(
                                project_id = "",
                                title = "Fabric API",
                                description = "Lightweight and modular API providing common hooks and intercompatibility measures utilized by mods using the Fabric toolchain.",
                                author = "modmuss50",
                                downloads = 160623387,
                                follows = 29828,
                                icon_url = "https://cdn.modrinth.com/data/P7dR8mSH/icon.png",
                                date_modified = "2026-04-17T16:46:00.525233+00:00",
                                clint_side = "optional",
                                server_side = "optional",
                                versions = emptyList()
                            )
                        ) {
                            s -> project = s
                        }
                    )
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
                .child(
                    UIComponents.button(
                        Component.literal("Back"),
                        { Minecraft.getInstance().setScreen(null) }
                    )
                )
                .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                .padding(Insets.of(10,10,10,10))
        )
        root.child(
            UIContainers
                .overlay(
                    UIContainers.verticalFlow(
                        Sizing.fill(50),
                        Sizing.fill(50)
                    )
                        .child(
                            UIComponents
                                .label(Component.literal("Are you sure you want to install this modification and its dependencies?"))
                                .color(Color.ofFormatting(ChatFormatting.RED))
                        )
                        .child(UIComponents.button(Component.literal("Yes"),{}))
                        .gap(8)
                        .surface(Surface.TOOLTIP)
                        .padding(Insets.of(24))
                        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                )
                    .closeOnClick(true)
                    .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                    .sizing(if(project != null) Sizing.fill() else Sizing.fill(0))
        )
    }

}