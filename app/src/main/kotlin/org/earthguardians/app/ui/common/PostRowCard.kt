package org.earthguardians.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.egPressScale
import org.earthguardians.app.ui.design.rememberPressInteraction
import org.earthguardians.app.ui.theme.egPalette

/**
 * Story row used across Home and the Stories tab:
 * thumbnail + title + date, iOS list-cell energy.
 */
@Composable
fun PostRowCard(
    post: EgPost,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = egPalette
    val interaction = rememberPressInteraction()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(EgRadius.tile))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .egPressScale(interaction)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EgImage(
            url = post.imageUrl,
            modifier = Modifier.size(72.dp),
            cornerRadius = 18.dp,
            contentDescription = post.title,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = post.title,
                color = palette.onBackground,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = RssParser.formatEpochShort(post.publishedAt),
                color = palette.muted,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = palette.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}
