package org.earthguardians.app.ui.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.earthguardians.app.ui.theme.egPalette

/** Spacing scale (iOS 4/8 grid). */
object EgSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** iOS-style corner radius scale. */
object EgRadius {
    val card = 24.dp
    val tile = 20.dp
    val pill = 100.dp
}

@Composable
fun rememberPressInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }

/** Springy press feedback, like iOS cell touches. */
fun Modifier.egPressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "egPressScale",
    )
    scale(scale)
}

/** Disposable haptic via LocalHapticFeedback — call inside composable context. */
@Composable
fun rememberTapHaptic(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}

/** Shimmer placeholder for async content (Apple style skeleton). */
fun Modifier.egShimmer(): Modifier = composed {
    val palette = egPalette
    val transition = rememberInfiniteTransition(label = "egShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "egShimmerAlpha",
    )
    background(palette.glass).alpha(alpha)
}

/**
 * The signature Earth Guardians glass card: translucent fill, hairline stroke,
 * and a soft top highlight — iOS "vibrancy" feel.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = EgRadius.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = egPalette
    val shape = RoundedCornerShape(cornerRadius)
    val interaction = rememberPressInteraction()

    val base = modifier
        .clip(shape)
        .background(palette.glass, shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(palette.glassHighlight, Color.Transparent),
                startY = 0f,
                endY = 260f,
            ),
            shape = shape,
        )
        .border(1.dp, palette.glassStroke, shape)
        .then(
            when (onClick) {
                null -> Modifier
                else -> Modifier.clickable(interactionSource = interaction, indication = null) { onClick() }
            },
        )
        .then(if (onClick != null) Modifier.egPressScale(interaction) else Modifier)

    Column(
        modifier = base.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Large-title header used across tabs (iOS largeTitle pattern). */
@Composable
fun LargeTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val palette = egPalette
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            color = palette.onBackground,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = palette.muted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = egPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = palette.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = palette.eco,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = rememberPressInteraction(),
                    indication = null,
                ) { onAction() },
            )
        }
    }
}

/** iOS segmented control — theme mode + in-app filters. */
@Composable
fun EgSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = egPalette
    val shape = RoundedCornerShape(EgRadius.pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.glass, shape)
            .border(1.dp, palette.glassStroke, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(EgRadius.pill))
                    .background(
                        when {
                            selected -> palette.onBackground
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(EgRadius.pill),
                    )
                    .clickable(
                        interactionSource = rememberPressInteraction(),
                        indication = null,
                    ) { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = option,
                    color = if (selected) palette.background else palette.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Apple-style pill button tones. Red = urgent action, Green = nature/positive. */
enum class PillTone { Neutral, Red, Green }

/** Shared iOS-style search field — theme-aware, ghost outline, red cursor. */
@Composable
fun EgSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val palette = egPalette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = palette.muted, fontSize = 15.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (value.isNotEmpty() && onClear != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = palette.muted,
                    )
                }
            }
        },
        shape = RoundedCornerShape(EgRadius.pill),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.eco,
            unfocusedBorderColor = palette.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = palette.accent,
            focusedTextColor = palette.onBackground,
            unfocusedTextColor = palette.onBackground,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun EgPrimaryButton(
    label: String,
    tone: PillTone = PillTone.Neutral,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    onClick: () -> Unit,
) {
    val palette = egPalette
    val bg = when (tone) {
        PillTone.Neutral -> palette.onBackground
        PillTone.Red -> palette.accent
        PillTone.Green -> palette.eco
    }
    val fg = when (tone) {
        PillTone.Neutral -> palette.background
        PillTone.Red -> palette.onAccent
        PillTone.Green -> palette.onEco
    }
    val interaction = rememberPressInteraction()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(EgRadius.pill))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .egPressScale(interaction)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (leading != null) {
            Icon(imageVector = leading, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EgPill(
    text: String,
    tone: PillTone = PillTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val palette = egPalette
    val bg = when (tone) {
        PillTone.Neutral -> palette.glass
        PillTone.Red -> palette.accent.copy(alpha = 0.16f)
        PillTone.Green -> palette.eco.copy(alpha = 0.16f)
    }
    val fg = when (tone) {
        PillTone.Neutral -> palette.onBackground
        PillTone.Red -> palette.accent
        PillTone.Green -> palette.eco
    }
    val shape = RoundedCornerShape(EgRadius.pill)
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Bento stat tile — the numbers that carry the mission. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Neutral,
) {
    val palette = egPalette
    val shape = RoundedCornerShape(EgRadius.tile)
    val accent = when (tone) {
        PillTone.Red -> palette.accent
        PillTone.Green -> palette.eco
        PillTone.Neutral -> palette.onBackground
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.glass, shape)
            .border(1.dp, palette.glassStroke, shape)
            .padding(14.dp),
    ) {
        Text(text = value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = palette.muted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Full-width circular icon + label tile used in the bento explore grid. */
@Composable
fun BentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Neutral,
    onClick: () -> Unit,
) {
    val palette = egPalette
    val interaction = rememberPressInteraction()
    val shape = RoundedCornerShape(EgRadius.tile)
    val accent = when (tone) {
        PillTone.Red -> palette.accent
        PillTone.Green -> palette.eco
        PillTone.Neutral -> palette.onBackground
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.glass, shape)
            .border(1.dp, palette.glassStroke, shape)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .egPressScale(interaction)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            color = palette.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = palette.muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Loading shimmer card shown while feeds sync. */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier, height: Dp = 96.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(EgRadius.card))
            .egShimmer(),
    )
}

/** Error state with retry — offline-first friendly. */
@Composable
fun FailedState(
    title: String = "You're offline",
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val palette = egPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = palette.muted,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(text = title, color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = palette.muted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(18.dp))
            EgPrimaryButton(label = "Try again", tone = PillTone.Red, onClick = onRetry)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val palette = egPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.muted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(text = title, color = palette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text = message, color = palette.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Chevron disclosure row (iOS settings style). */
@Composable
fun DisclosureRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = egPalette
    val interaction = rememberPressInteraction()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 12.dp, horizontal = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, color = palette.muted, fontSize = 13.sp)
        }
    }
}
