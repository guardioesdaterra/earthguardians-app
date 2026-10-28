package org.earthguardians.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.design.egPressScale
import org.earthguardians.app.ui.design.rememberPressInteraction
import org.earthguardians.app.ui.theme.egPalette

/** Image with brand-gradient fallback (photos from Squarespace CDN when present). */
@Composable
fun EgImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerRadius: Dp = EgRadius.tile,
) {
    val palette = egPalette
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier.clip(shape).background(palette.glass, shape)) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                palette.accent.copy(alpha = 0.18f),
                                palette.eco.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
        }
    }
}
