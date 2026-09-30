package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.ui.components.TivionsCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.TitleNormalizer

data class CastMember(
    val id: String,
    val name: String,
    val character: String = "",
    val profileUrl: String? = null
)

@Composable
fun DetailScreen(
    item: PlaylistItem,
    availableSeasons: List<Int> = emptyList(),
    episodes: List<PlaylistItem> = emptyList(),
    selectedSeason: Int = 1,
    onSelectSeason: (Int) -> Unit = {},
    onBack: () -> Unit,
    onPlay: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    onToggleDownload: (PlaylistItem) -> Unit
) {
    val context = LocalContext.current

    // Ekran başlığı (API adı veya temizlenmiş film adı)
    val displayTitle = remember(item.name) {
        TitleNormalizer.extractMovieMeta(item.name).cleanTitle.takeIf { it.isNotBlank() }
            ?: item.name
    }

    // Tür ve yıl formatı: Örn. "Aksiyon, Komedi, Suç • 2025"
    val genreAndYearText = remember(item.category, item.year, item.name) {
        val genrePart = item.category.takeIf { it.isNotBlank() && it != "Genel" } ?: "Film"
        val yearPart = item.year.takeIf { it.isNotBlank() && it != "2024" }
            ?: TitleNormalizer.extractMovieMeta(item.name).year
            ?: "2025"
        "$genrePart • $yearPart"
    }

    // Özet açıklama (API'den gelen açıklama öncelikli)
    val displayOverview = remember(item.description) {
        item.description.takeIf { it.isNotBlank() && !it.endsWith("Filmi") && !it.endsWith("Dizisi") && it != "Film özeti hazırlanıyor..." }
            ?: item.description.takeIf { it.isNotBlank() }
            ?: "Film özeti hazırlanıyor..."
    }

    // Oyuncu kadrosu (Xtream API oyuncuları)
    val castMembers = remember(item.cast) {
        if (item.cast.isNotBlank()) {
            item.cast.split(",", ";").mapIndexed { idx, name ->
                CastMember(
                    id = "$idx",
                    name = name.trim(),
                    character = "",
                    profileUrl = null
                )
            }.filter { it.name.isNotBlank() }.take(15)
        } else {
            emptyList()
        }
    }

    // Sezon listesi (Diziler için)
    val seasonsToDisplay = remember(availableSeasons, item) {
        if (availableSeasons.isNotEmpty()) availableSeasons
        else if (item.seasonNumber > 1) (1..item.seasonNumber).toList()
        else listOf(1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("detail_scroll_container"),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. ÜST KISIM — FRAGMAN (TRAILER) OYNATICI (16:9) + OVERLAY GERİ BUTONU
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    TrailerPlayerView(
                        trailerKey = item.subtitleInfo.takeIf { it.startsWith("yt:") }?.removePrefix("yt:")
                            ?: item.subtitleInfo.takeIf { it.isNotBlank() },
                        backdropUrl = item.downloadLocalPath?.takeIf { it.startsWith("http") }
                            ?: item.logoUrl,
                        fallbackPosterUrl = item.logoUrl,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Sol üst köşe — Yarı saydam gri daire overlay geri butonu
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 16.dp, top = 8.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x991E243A))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBack() }
                            .testTag("btn_detail_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. FRAGMANIN HEMEN ALTINDAKİ BİLGİ ALANI
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Film Adı (büyük, kalın yazı) — API name alanı
                    Text(
                        text = displayTitle,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp,
                        modifier = Modifier.testTag("detail_movie_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tür, yapım yılı (film adının hemen altında, orta boy gri/soluk yazı) — Aksiyon, Komedi, Suç • 2025
                    Text(
                        text = genreAndYearText,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.testTag("detail_movie_genre_year")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // "Şimdi İzle" butonu (geniş, beyaz/dolgun arka planlı, play ikonu ile birlikte)
                    Button(
                        onClick = { onPlay(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_detail_watch_now")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Şimdi İzle",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Alt butonlar satırı — 3 buton yan yana: Favori (Listem) / İndir / Paylaş
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Favori butonu (kalp ikonu, "Listem" / "Listemde")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E243A))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { onToggleFavorite(item) }
                                .testTag("btn_detail_fav"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favori",
                                    tint = if (item.isFavorite) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (item.isFavorite) "Listemde" else "Listem",
                                    color = if (item.isFavorite) Color(0xFFEF4444) else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 2. İndir butonu (indirme ikonu, "İndir" / "İndirildi")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E243A))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { onToggleDownload(item) }
                                .testTag("btn_detail_download"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isDownloaded) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = "İndir",
                                    tint = if (item.isDownloaded) AccentCyan else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (item.isDownloaded) "İndirildi" else "İndir",
                                    color = if (item.isDownloaded) AccentCyan else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 3. Paylaş butonu (paylaş ikonu, "Paylaş")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E243A))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "$displayTitle ($genreAndYearText)\n\n$displayOverview\n\nTivions IPTV ile izle!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Filmi Paylaş"))
                                }
                                .testTag("btn_detail_share"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Paylaş",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Filmin açıklaması (özet) — API plot/description alanı
                    Text(
                        text = displayOverview,
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.testTag("detail_movie_overview")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Oyuncular (Cast) Bölümü — Açıklamanın hemen altında, yatay kaydırılabilen liste
                    if (castMembers.isNotEmpty()) {
                        Text(
                            text = "Oyuncular",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.testTag("detail_cast_list")
                        ) {
                            items(castMembers, key = { it.id }) { actor ->
                                CastMemberItem(actor = actor)
                            }
                        }
                    }
                }
            }

            // Dizi ise: Sezonlar ve Bölümler Bölümü
            if (item.type == ItemType.VOD_SERIES) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Sezonlar",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(seasonsToDisplay) { season ->
                                val isSelected = selectedSeason == season
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8)))
                                            else Brush.linearGradient(listOf(Color(0xFF1E243A), Color(0xFF1E243A)))
                                        )
                                        .clickable { onSelectSeason(season) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "$season. Sezon",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Bölümler",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (episodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$selectedSeason. Sezon için bölüm bulunamadı.",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(episodes, key = { it.id }) { ep ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            EpisodeCard(
                                episode = ep,
                                onPlay = { onPlay(ep) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 16:9 Dikdörtgen Fragman Oynatıcı.
 * YouTube Trailer embed olarak sessiz, otomatik oynatma, kontroller gizli ve loop modunda oynatılır.
 * Fragman yoksa statik backdrop veya poster görseli fallback olarak gösterilir.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerPlayerView(
    trailerKey: String?,
    backdropUrl: String?,
    fallbackPosterUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (!trailerKey.isNullOrBlank()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return true // İçerikten ayrılmayı engelle
                            }
                        }
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { webView ->
                    // YouTube embed parametreleri: autoplay=1&mute=1&controls=0&loop=1&playlist={key}&showinfo=0&modestbranding=1
                    val embedHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body { width: 100%; height: 100%; overflow: hidden; background: #000000; }
                                .video-container { position: relative; width: 100vw; height: 100vh; overflow: hidden; }
                                iframe { width: 100%; height: 100%; border: none; pointer-events: none; }
                            </style>
                        </head>
                        <body>
                            <div class="video-container">
                                <iframe 
                                    src="https://www.youtube.com/embed/$trailerKey?autoplay=1&mute=1&controls=0&loop=1&playlist=$trailerKey&showinfo=0&modestbranding=1&rel=0&iv_load_policy=3&playsinline=1"
                                    allow="autoplay; encrypted-media"
                                    allowfullscreen>
                                </iframe>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                },
                onRelease = { webView ->
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.destroy()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fragman yoksa: backdrop veya poster görseli (fallback)
            val imageUrl = backdropUrl?.takeIf { it.isNotBlank() }
                ?: fallbackPosterUrl?.takeIf { !it.isNullOrBlank() }

            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E243A), Color(0xFF0F1424))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Alt tarafta içerikle kusursuz geçiş sağlayan hafif karartma gradyanı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0F1424))
                    )
                )
        )
    }
}

/**
 * Oyuncu Kadrosu Kartı.
 * Yuvarlak profil fotoğrafı, kişi silüeti placeholder, oyuncu adı ve karakter adı.
 */
@Composable
fun CastMemberItem(
    actor: CastMember
) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .testTag("cast_member_${actor.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E243A))
                .border(1.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val photoUrl = actor.profileUrl
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = actor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fotoğraf yoksa varsayılan kişi silüeti
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = actor.name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (actor.character.isNotBlank()) {
            Text(
                text = actor.character,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EpisodeCard(
    episode: PlaylistItem,
    onPlay: () -> Unit
) {
    TivionsCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("episode_${episode.seasonNumber}_${episode.episodeNumber}"),
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F243B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B${episode.episodeNumber}",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${episode.duration.ifBlank { "45 dk" }} • Sezon ${episode.seasonNumber}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C314E))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Bölümü Oynat",
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
