package org.gaziz.modrinthdirect.client.ui.components

import com.terraformersmc.modmenu.gui.ModsScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class BottomRow(
    previous: Screen,
    current: Screen
): FlowLayout(
    Sizing.fill(),
    Sizing.fill(13),
    Algorithm.HORIZONTAL
) {
    val installBtn: ButtonComponent = UIComponents
        .button(
            Text.literal("Install mod")
        ) {
            it.active(false)
        }
        .active(false)

    private val backButton = UIComponents.button(
        Text.literal("Back")
    ) {
        MinecraftClient.getInstance().setScreen(previous)
    }

    private val filterButton = UIComponents
        .button(
            Text.literal("Installed mods")
        ) {}

    init {
        this.child(
            UIContainers.horizontalFlow(Sizing.fill(33), Sizing.content())
                .child(filterButton)
                .horizontalAlignment(HorizontalAlignment.LEFT)
        )
        this.child(
            UIContainers
                .horizontalFlow(Sizing.fill(33), Sizing.content())
                .child(backButton)
                .child(installBtn)
                .gap(16)
                .horizontalAlignment(HorizontalAlignment.CENTER)
        )
        this.child(
            UIContainers.horizontalFlow(Sizing.fill(33), Sizing.content())
                .child(
                    UIComponents.button(
                        Text.literal("Options")
                    ) {
                        MinecraftClient.getInstance().setScreen(ModsScreen(current))
                    }
                )
                .horizontalAlignment(HorizontalAlignment.RIGHT)
        )
        this.verticalAlignment(VerticalAlignment.CENTER)
        this.horizontalAlignment(HorizontalAlignment.CENTER)
    }
}
