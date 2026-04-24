package org.gaziz.modrinthdirect.client.mixin

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import org.gaziz.modrinthdirect.client.ModificationsScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(TitleScreen::class)
abstract class TitleScreenMixin: Screen(Component.literal("")) {
    @Inject(at = [At("TAIL")], method = ["init"])
    private fun onInit(info: CallbackInfo) {
        val width = this.width/2
        val height = this.height/12
        this.addRenderableWidget(Button.builder(Component.literal("Install mods"),{
            Minecraft.getInstance().setScreen(ModificationsScreen)
        })
            .size(width,height)
            .pos((this.width / 2)-width/2, (this.height / 2)+53)
            .build())
    }
}