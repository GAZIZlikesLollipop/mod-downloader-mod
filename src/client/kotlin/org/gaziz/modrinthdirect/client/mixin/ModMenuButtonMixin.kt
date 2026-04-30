package org.gaziz.modrinthdirect.client.mixin

import com.terraformersmc.modmenu.gui.widget.ModMenuButtonWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import org.gaziz.modrinthdirect.ModrinthDirect
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen
import org.slf4j.LoggerFactory
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(TitleScreen::class)
abstract class TitleScreenMixin: Screen(Text.literal("")) {
    private val logger = LoggerFactory.getLogger(ModrinthDirect.MOD_ID)
    @Inject(at = [At("TAIL")], method = ["init"])
    private fun onInit(info: CallbackInfo) {
        val width = this.width/2
        val height = this.height/12
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Install mods")) {
            logger.info("tpasd;fkjasdf")
            MinecraftClient.getInstance().setScreen(ModificationsScreen)
        }
            .size(width,height)
            .position((this.width / 2)-width/2, (this.height / 2)+53)
            .build())
    }
}

@Mixin(ModMenuButtonWidget::class)
abstract class ModMenuButtonMixin: ButtonWidget.Text(0,0,0,0, net.minecraft.text.Text.empty(),{}, DEFAULT_NARRATION_SUPPLIER) {
//    private companion object {
//        private const val COMP_CONST = "Lnet/minecraft/text/Text"
//        private const val SCREEN_CONST = "Lnet/minecraft/client/gui/screen/Screen"
//        private const val BUTTON_CONST = "Lcom/terraformersmc/modmenu/gui/widget/ModMenuButtonWidget"
//    }
//    @ModifyArg(
//        method = ["<init>"],
//        at = At(
//            value = "INVOKE",
//            target = "$BUTTON_CONST;<init>(IIII$COMP_CONST$SCREEN_CONST)$BUTTON_CONST"
//        ),
//        index = 5
//    )
//    private fun onClick(): Screen {
//        return ModificationsScreen
//    }
}