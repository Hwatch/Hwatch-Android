package com.hwatch.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.hwatch.app.util.ThumbnailUrl

/**
 * 统一的视频缩略图组件（Library / Dashboard 共用）。
 *
 * - 使用 `GET /thumb/{video_id}`，严格复用 [ThumbnailUrl]（含 video_id 编码），
 *   不使用 `thumbnail_url`、不做字符串拼 URL。
 * - 16:9 + [ContentScale.Crop]，loading/error 使用统一占位（SVG 由 ImageLoader 的
 *   SvgDecoder 处理）。
 * - 使用共享 Coil ImageLoader（AppContainer 单例），不创建第二套网络栈。
 */
@Composable
fun VideoThumbnail(
    baseUrl: String,
    videoId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(HWatchShapes.thumbnail)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val url = remember(baseUrl, videoId) { ThumbnailUrl.build(baseUrl, videoId) }
        if (url == null) {
            ThumbnailPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ThumbnailPlaceholder() },
                error = { ThumbnailPlaceholder() },
            )
        }
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
