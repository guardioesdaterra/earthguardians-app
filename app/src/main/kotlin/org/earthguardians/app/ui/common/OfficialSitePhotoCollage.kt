package org.earthguardians.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A quiet photo wallpaper: a brick-bond 2×4 grid cropped from the official
 * catalog. Tiles align edge-to-edge (never overlap or rotate); the odd row is
 * shifted half a tile so the texture still reads as hand-set rather than tiled.
 */
@Composable
internal fun OfficialSitePhotoCollage(
    page: Int,
    modifier: Modifier = Modifier,
    opacity: Float = 0.075f,
) {
    val rows = 2
    val columns = 4
    val gap = 6.dp
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val tileWidth = if (maxWidth == Dp.Infinity) 0.dp else (maxWidth - gap * (columns - 1)) / columns
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(rows) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = if (row == 1) tileWidth / 2 else 0.dp),
                ) {
                    repeat(columns) { column ->
                        Image(
                            painter = painterResource(
                                OfficialSiteImages.COLLAGE[((row * columns + column) + page) % OfficialSiteImages.COLLAGE.size],
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .graphicsLayer { alpha = opacity },
                        )
                    }
                }
            }
        }
    }
}
