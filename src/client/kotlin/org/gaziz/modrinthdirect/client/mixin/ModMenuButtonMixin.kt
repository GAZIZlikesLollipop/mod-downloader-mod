package org.gaziz.modrinthdirect.client.mixin

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ButtonWidget::class)
abstract class ModMenuButtonMixin {

    @Shadow
    @Final
    @Mutable
    protected lateinit var onPress: ButtonWidget.PressAction

    @Inject(method = ["<init>"],at = [At("TAIL")])
    private fun onInit(x: Int, y: Int,width: Int, height: Int,text: Text,press: ButtonWidget.PressAction,nar: ButtonWidget.NarrationSupplier,info: CallbackInfo) {
        if(text.string == Text.translatable("modmenu.title").string){
            (this as ButtonWidget).apply {
                onPress = {
                    MinecraftClient.getInstance().setScreen(ModificationsScreen(MinecraftClient.getInstance().currentScreen ?: TitleScreen()))
                }
            }
        }
    }

}