package org.earthguardians.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class PhotoPlacement(
    val alignment: Alignment,
    val width: Dp,
    val height: Dp,
    val x: Dp,
    val y: Dp,
    val rotation: Float,
)

private val photoPlacements = listOf(
    PhotoPlacement(Alignment.TopStart, 190.dp, 132.dp, (-54).dp, 96.dp, -9f),
    PhotoPlacement(Alignment.TopEnd, 178.dp, 124.dp, 52.dp, 112.dp, 8f),
    PhotoPlacement(Alignment.CenterStart, 175.dp, 126.dp, (-62).dp, (-4).dp, 6f),
    PhotoPlacement(Alignment.CenterEnd, 196.dp, 134.dp, 56.dp, 6.dp, -7f),
    PhotoPlacement(Alignment.BottomStart, 205.dp, 140.dp, (-52).dp, 82.dp, 7f),
    PhotoPlacement(Alignment.BottomEnd, 190.dp, 132.dp, 48.dp, 90.dp, -8f),
    PhotoPlacement(Alignment.Center, 188.dp, 132.dp, 0.dp, 0.dp, 3f),
    PhotoPlacement(Alignment.TopCenter, 170.dp, 116.dp, 0.dp, (-64).dp, -4f),
)

/** A subtle multi-photo wallpaper; foreground copy and controls stay opaque. */
@Composable
internal fun OfficialSitePhotoCollage(
    page: Int,
    modifier: Modifier = Modifier,
    opacity: Float = 0.075f,
) {
    Box(modifier.clipToBounds()) {
        photoPlacements.forEachIndexed { index, placement ->
            Image(
                painter = painterResource(OfficialSiteImages.COLLAGE[(index + page) % OfficialSiteImages.COLLAGE.size]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(placement.alignment)
                    .offset(x = placement.x, y = placement.y)
                    .size(width = placement.width, height = placement.height)
                    .clip(RoundedCornerShape(16.dp))
                    .graphicsLayer {
                        alpha = opacity
                        rotationZ = placement.rotation + page * 2f
                    },
            )
        }
    }
}
