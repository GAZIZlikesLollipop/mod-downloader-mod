package org.gaziz.modrinthdirect.client.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.util.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import org.gaziz.modrinthdirect.client.api.ApiClient
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen.intermediateChild

class SearchRow(
    installBtn: ButtonComponent,
    modList: FlowLayout
): FlowLayout(
    Sizing.content(),
    Sizing.fill(12),
    Algorithm.HORIZONTAL
) {
   init {
       val searchText: Observable<String> = Observable.of("")
       val searchButton = UIComponents
           .button(
               Component.literal("   ")
           ) {
               Minecraft.getInstance().execute {
                   installBtn.message = Component.literal("Install mod")
                   installBtn.active(false)
                   modList.clearChildren()
                   modList.child(intermediateChild)
               }
               CoroutineScope(Dispatchers.IO).launch {
                   ApiClient.search(searchText.get())
               }
           }
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
       this.child(searchField)
           .child(
               UIContainers
                   .stack(Sizing.content(), Sizing.content())
                   .child(searchButton)
                   .child(
                       UIComponents
                           .item(Items.SPYGLASS.defaultInstance)
                           .sizing(Sizing.fixed(16))
                   )
                   .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
           )
           .gap(2)
           .verticalAlignment(VerticalAlignment.CENTER)
   }
}

class BottomRow: FlowLayout(
    Sizing.fill(),
    Sizing.fill(13),
    Algorithm.HORIZONTAL
) {
    val installBtn: ButtonComponent = UIComponents
        .button(
            Component.literal("Install mod"),
            {
                it.active(false)
            }
        )
        .active(false)
    init {
        this.child(
            UIComponents.button(
                Component.literal("Back"),
                { Minecraft.getInstance().setScreen(null) }
            )
        )
        this.child(installBtn)
        this.gap(16)
        this.verticalAlignment(VerticalAlignment.CENTER)
        this.horizontalAlignment(HorizontalAlignment.CENTER)
    }
}