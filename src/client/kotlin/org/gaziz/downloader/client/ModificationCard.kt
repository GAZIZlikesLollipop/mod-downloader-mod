package org.gaziz.downloader.client

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.component.UIComponents.texture
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.util.Observable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items
import org.gaziz.downloader.Moddownloadermod
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.pow

class ModificationCard(
    hit: SearchHit,
    project: Observable<String?>,
    texturePath: Observable<String>,
    changeProject: (String) -> Unit,
): FlowLayout(
    Sizing.content(),
    Sizing.content(),
    Algorithm.HORIZONTAL
) {
    private fun formatTimeAgo(isoTimestamp: String): String {
        return try {
            val past = OffsetDateTime.parse(isoTimestamp).toInstant()
            val now = Instant.now()
            val seconds = Duration.between(past, now).seconds

            when {
                seconds < 60 -> "just now"
                seconds < 3600 -> {
                    val minutes = seconds / 60
                    if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
                }
                seconds < 86400 -> {
                    val hours = seconds / 3600
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
                seconds < 172800 -> "yesterday"
                seconds < 604800 -> {
                    val days = seconds / 86400
                    "$days days ago"
                }
                seconds < 1209600 -> "last week"
                seconds < 2592000 -> {
                    val weeks = seconds / 604800
                    "$weeks weeks ago"
                }
                seconds < 31536000 -> {
                    val months = seconds / 2592000
                    if (months == 1L) "last month" else "$months months ago"
                }
                else -> {
                    val years = seconds / 31536000
                    if (years == 1L) "last year" else "$years years ago"
                }
            }
        } catch (_: Exception) {
            "unknown date"
        }
    }
    private fun Long.toDisplay(): String {
        val str = this.toString()
        return when (this) {
            in 10000..99999 -> String.format("%.2fK",this/10.0.pow(3))
            in 100000..999999 -> String.format("%.2fK",this/10.0.pow(3))
            in 1000000..99999999 -> String.format("%.2fM",this/10.0.pow(6))
            in 10000000..999999999 -> String.format("%.2fM",this/10.0.pow(6))
            in 100000000..9999999999 -> String.format("%.2fM",this/10.0.pow(6))
            else -> str
        }
    }

    init {
        var modType = ""
        if(hit.client_side == "optional" || hit.client_side == "required") {
            modType = "Client, "
        }
        if(hit.server_side == "optional" || hit.server_side == "required") {
            modType += "Server"
        } else {
            modType = modType.dropLast(2)
        }

        this.surface(Surface.TOOLTIP)
        this.padding(Insets.of(16))
        this.verticalAlignment(VerticalAlignment.CENTER)
        this.gap(12)

        val lastChild =
            texture(
                Identifier.fromNamespaceAndPath(
                    Moddownloadermod.MOD_ID,
                    texturePath.get()
                ),
                0,
                0,
                64,
                64,
                64,
                64
            ).sizing(Sizing.fixed(32))

        this.child(lastChild)

        texturePath.observe {
            this.removeChild(lastChild)
            this.child(
                0,
                texture(
                    Identifier.fromNamespaceAndPath(
                        Moddownloadermod.MOD_ID,
                        it
                    ),
                    0,
                    0,
                    64,
                    64,
                    64,
                    64
                )
                    .sizing(Sizing.fixed(32))
            )
        }

        this.child(
            UIContainers.verticalFlow(Sizing.fill(65), Sizing.content())
                .child(
                    UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(UIComponents.label(Component.literal(hit.title)))
                        .child(
                            UIComponents
                                .label(Component.literal(" by ${hit.author}"))
                                .color(Color.ofFormatting(ChatFormatting.GRAY))
                        )
                )
                .child(
                    UIComponents
                        .label(Component.literal(hit.description))
                        .color(Color.ofFormatting(ChatFormatting.GRAY))
                )
                .child(
                    UIComponents
                        .label(Component.literal(modType))
                        .color(Color.ofFormatting(ChatFormatting.DARK_GRAY))
                )
                .gap(4)
        )
        this.child(
            UIContainers.verticalFlow(
                Sizing.fill(21),
                Sizing.content()
            )
                .child(
                    UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(UIComponents.item(Items.HOPPER.defaultInstance).sizing(Sizing.fixed(10)))
                        .child(UIComponents.label(Component.literal(hit.downloads.toDisplay())))
                        .child(UIComponents.item(Items.NETHER_STAR.defaultInstance).sizing(Sizing.fixed(10)))
                        .child(UIComponents.label(Component.literal(hit.follows.toDisplay())))
                        .gap(2)
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .child(
                    UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(UIComponents.item(Items.CLOCK.defaultInstance).sizing(Sizing.fixed(10)))
                        .child(
                            UIComponents
                                .label(Component.literal(formatTimeAgo(hit.date_modified)))
                                .color(Color.ofFormatting(ChatFormatting.GRAY))
                        )
                        .gap(2)
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .child(
                    UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                        .child(UIComponents.item(Items.GLASS_BOTTLE.defaultInstance).sizing(Sizing.fixed(10)))
                        .child(
                            UIComponents
                                .label(Component.literal("Not installed"))
                                .color(Color.ofFormatting(ChatFormatting.RED))
                        )
                        .gap(2)
                        .verticalAlignment(VerticalAlignment.CENTER)
                )
                .gap(4)
        )
        this.mouseDown().subscribe { _, bool ->
            changeProject(hit.project_id)
            this.surface(Surface.DARK_PANEL)
            bool
        }
        this.mouseEnter().subscribe {
            this.surface(Surface.DARK_PANEL)
        }
        project.observe { p ->
           if(p != hit.project_id)  {
               this.surface(Surface.TOOLTIP)
           }
        }
        this.mouseLeave().subscribe {
            val project = project.get()
            if(project != null) {
                if(project != hit.project_id) {
                    this.surface(Surface.TOOLTIP)
                }
            } else {
                this.surface(Surface.TOOLTIP)
            }
        }
    }
}