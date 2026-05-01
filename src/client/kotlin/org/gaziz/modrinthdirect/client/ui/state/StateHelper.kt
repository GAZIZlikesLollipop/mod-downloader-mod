package org.gaziz.modrinthdirect.client.ui.state

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.minecraft.item.Items
import net.minecraft.text.Text
import org.gaziz.modrinthdirect.client.api.ApiClient

object StateHelper {

    init {
        CoroutineScope(Dispatchers.Default).launch {
            ApiClient.search("")
        }
    }

    fun formatTitle(title: String) = "[a-z0-9/._-]"
        .toRegex()
        .findAll(
            title
                .lowercase()
                .replace(" ","-"),
            0
        )
        .joinToString("") { it.value }

    val intermediateChild: ParentUIComponent = UIContainers
        .verticalFlow(
            Sizing.fill(),
            Sizing.fill(85)
        )
        .child(UIComponents.item(Items.CLOCK.defaultStack))
        .child(UIComponents.label(Text.literal("Loading...")))
        .gap(6)
        .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

    val cachedIcons = MutableStateFlow<Map<String,String?>>(emptyMap())

    val isInstalledMods = MutableStateFlow(false)
}