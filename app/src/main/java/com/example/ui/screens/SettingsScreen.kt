package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.UserProfile
import com.example.ui.components.ParentalPinDialog
import com.example.ui.components.TivionsCard
import com.example.ui.components.TivionsTopBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BgCardDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * BÖLÜM 15: Tivions Ayarlar Ekranı (Settings Screen)
 * 11 accordion/expandable sections:
 * 1. Genel
 * 2. Kaynaklar (Playlist Yönetimi)
 * 3. Otomatik Yenileme
 * 4. Oynatıcı
 * 5. EPG
 * 6. Ağ ve Buffer
 * 7. İndirmeler
 * 8. Bildirimler
 * 9. Ebeveyn Kontrolü
 * 10. Depolama ve Veri
 * 11. Hakkında
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    profiles: List<UserProfile> = emptyList(),
    activeProfile: UserProfile? = null,
    onUpdateSettings: (AppSettings) -> Unit,
    onSwitchProfile: (String) -> Unit = {},
    onOpenConnectSource: () -> Unit,
    onRefreshEpg: () -> Unit = {},
    onBack: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>("genel") }
    var showPinDialog by remember { mutableStateOf(false) }

    fun toggle(section: String) {
        expandedSection = if (expandedSection == section) null else section
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TivionsTopBar(
                title = "Ayarlar",
                showBack = true,
                showCast = false,
                showSettings = false,
                onBackClick = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: Profil Seçici (Üst kısım)
                if (profiles.isNotEmpty()) {
                    item {
                        TivionsCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Aktif Profil",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    profiles.forEach { prof ->
                                        val isActive = prof.id == activeProfile?.id
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isActive) AccentPurple else BgCardDark)
                                                .border(1.dp, if (isActive) AccentPurple else BorderSubtle, RoundedCornerShape(20.dp))
                                                .clickable { onSwitchProfile(prof.id) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(prof.avatarColorHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(prof.name.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(prof.name, color = if (isActive) Color.White else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 1. GENEL
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Genel",
                        icon = Icons.Default.Settings,
                        isExpanded = expandedSection == "genel",
                        onToggle = { toggle("genel") }
                    ) {
                        SettingsRowText(
                            title = "Uygulama Dili",
                            value = settings.language,
                            onClick = {
                                val next = if (settings.language == "Türkçe") "English" else "Türkçe"
                                onUpdateSettings(settings.copy(language = next))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Uygulama Açılış Sekmesi",
                            value = "Anasayfa",
                            onClick = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Çift Geri Tuşu ile Çıkış",
                            subtitle = "Yanlışlıkla çıkışı önlemek için iki kez dokunun",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }

                // ==========================================
                // 2. KAYNAKLAR (Playlist Yönetimi)
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Kaynaklar (Playlist Yönetimi)",
                        icon = Icons.Default.PlaylistAdd,
                        isExpanded = expandedSection == "kaynaklar",
                        onToggle = { toggle("kaynaklar") }
                    ) {
                        SettingsRowText(
                            title = "Yeni Kaynak Ekle / Değiştir",
                            value = "Bağla",
                            onClick = onOpenConnectSource
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Playlisti Şimdi Yenile",
                            value = "Güncelle",
                            onClick = onRefreshEpg
                        )
                    }
                }

                // ==========================================
                // 3. OTOMATİK YENİLEME
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Otomatik Yenileme",
                        icon = Icons.Default.Refresh,
                        isExpanded = expandedSection == "otomatik_yenileme",
                        onToggle = { toggle("otomatik_yenileme") }
                    ) {
                        SettingsRowText(
                            title = "Yenileme Sıklığı",
                            value = settings.autoRefreshInterval,
                            onClick = {
                                val options = listOf("Günlük", "3 Günlük", "1 Haftalık", "Uygulama Girişinde", "Kapalı")
                                val nextIdx = (options.indexOf(settings.autoRefreshInterval) + 1) % options.size
                                onUpdateSettings(settings.copy(autoRefreshInterval = options[nextIdx]))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Sadece Wi-Fi'de Yenile",
                            subtitle = "Mobil veri kotanızı korur",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }

                // ==========================================
                // 4. OYNATICI
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Oynatıcı",
                        icon = Icons.Default.PlayCircle,
                        isExpanded = expandedSection == "oynatici",
                        onToggle = { toggle("oynatici") }
                    ) {
                        SettingsRowText(
                            title = "Canlı Yayın Oynatıcı",
                            value = settings.livePlayerEngine,
                            onClick = {
                                val next = if (settings.livePlayerEngine == "Advanced Exo Player") "Default Exo Player" else "Advanced Exo Player"
                                onUpdateSettings(settings.copy(livePlayerEngine = next))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Canlı Akış Formatı",
                            value = settings.liveStreamFormat,
                            onClick = {
                                val next = if (settings.liveStreamFormat == ".ts") ".m3u8" else ".ts"
                                onUpdateSettings(settings.copy(liveStreamFormat = next))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Film & Dizi Oynatıcı",
                            value = settings.videoPlayerEngine,
                            onClick = {
                                val next = if (settings.videoPlayerEngine == "EXO") "VLC Player" else "EXO"
                                onUpdateSettings(settings.copy(videoPlayerEngine = next))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Arka Planda Ses Oynatma",
                            subtitle = "Uygulama kapalıyken yayını dinleyin",
                            checked = settings.backgroundPlayback,
                            onCheckedChange = { onUpdateSettings(settings.copy(backgroundPlayback = it)) }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Çift Dokunma Sarma Süresi",
                            value = "10 saniye",
                            onClick = {}
                        )
                    }
                }

                // ==========================================
                // 5. EPG
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "EPG (Yayın Akışı)",
                        icon = Icons.Default.Tv,
                        isExpanded = expandedSection == "epg",
                        onToggle = { toggle("epg") }
                    ) {
                        SettingsRowText(
                            title = "EPG Program Rehberini Yenile",
                            value = "Şimdi Güncelle",
                            onClick = onRefreshEpg
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Otomatik Eşleştirme",
                            subtitle = "Kanal ID ve adına göre programları bulur",
                            checked = settings.epgAutoMatch,
                            onCheckedChange = { onUpdateSettings(settings.copy(epgAutoMatch = it)) }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "EPG Önbelleği",
                            subtitle = "Programları çevrimdışı kullanım için sakla",
                            checked = settings.epgCache,
                            onCheckedChange = { onUpdateSettings(settings.copy(epgCache = it)) }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "EPG Zaman Kaydırma",
                            value = "${settings.epgTimeShiftMinutes} dakika",
                            onClick = {
                                val next = (settings.epgTimeShiftMinutes + 30) % 180
                                onUpdateSettings(settings.copy(epgTimeShiftMinutes = next))
                            }
                        )
                    }
                }

                // ==========================================
                // 6. AĞ VE BUFFER
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Ağ ve Buffer",
                        icon = Icons.Default.Wifi,
                        isExpanded = expandedSection == "ag_buffer",
                        onToggle = { toggle("ag_buffer") }
                    ) {
                        SettingsRowText(
                            title = "Buffer Boyutu",
                            value = settings.bufferSetting,
                            onClick = {
                                val opts = listOf("Otomatik", "3 saniye", "5 saniye", "7 saniye")
                                val nxt = (opts.indexOf(settings.bufferSetting) + 1) % opts.size
                                onUpdateSettings(settings.copy(bufferSetting = opts[nxt]))
                            }
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Mobil Veri Uyarısı",
                            subtitle = "Wi-Fi dışındayken video başlatmadan önce uyar",
                            checked = false,
                            onCheckedChange = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "User-Agent",
                            value = "Tivions/1.0",
                            onClick = {}
                        )
                    }
                }

                // ==========================================
                // 7. İNDİRMELER
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "İndirmeler",
                        icon = Icons.Default.CloudDownload,
                        isExpanded = expandedSection == "indirmeler",
                        onToggle = { toggle("indirmeler") }
                    ) {
                        SettingsRowSwitch(
                            title = "Sadece Wi-Fi'de İndir",
                            subtitle = "Hücresel veri kullanımını engeller",
                            checked = true,
                            onCheckedChange = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "İndirme Kalitesi",
                            value = "En Yüksek",
                            onClick = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Depolama Konumu",
                            value = "Dahili Hafıza",
                            onClick = {}
                        )
                    }
                }

                // ==========================================
                // 8. BİLDİRİMLER
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Bildirimler",
                        icon = Icons.Default.Notifications,
                        isExpanded = expandedSection == "bildirimler",
                        onToggle = { toggle("bildirimler") }
                    ) {
                        SettingsRowSwitch(
                            title = "Program Hatırlatıcıları",
                            subtitle = "Seçilen TV programı başlamadan 5 dk önce uyar",
                            checked = true,
                            onCheckedChange = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowSwitch(
                            title = "Yeni Bölüm Bildirimleri",
                            subtitle = "Takip edilen dizilere yeni bölüm eklendiğinde haber ver",
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }

                // ==========================================
                // 9. EBEVEYN KONTROLÜ
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Ebeveyn Kontrolü",
                        icon = Icons.Default.Lock,
                        isExpanded = expandedSection == "ebeveyn",
                        onToggle = { toggle("ebeveyn") }
                    ) {
                        SettingsRowSwitch(
                            title = "PIN Koruması",
                            subtitle = "Yetişkin kategoriler veya kilitli profiller için PIN sor",
                            checked = settings.isParentalControlEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) showPinDialog = true
                                else onUpdateSettings(settings.copy(isParentalControlEnabled = false))
                            }
                        )
                        if (settings.isParentalControlEnabled) {
                            HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                            SettingsRowText(
                                title = "PIN Kodunu Değiştir",
                                value = "••••",
                                onClick = { showPinDialog = true }
                            )
                        }
                    }
                }

                // ==========================================
                // 10. DEPOLAMA VE VERİ
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Depolama ve Veri",
                        icon = Icons.Default.SdStorage,
                        isExpanded = expandedSection == "depolama",
                        onToggle = { toggle("depolama") }
                    ) {
                        SettingsRowText(
                            title = "Önbelleği Temizle",
                            value = "24.5 MB",
                            onClick = {}
                        )
                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        SettingsRowText(
                            title = "Veritabanını Sıfırla",
                            value = "Sıfırla",
                            onClick = {}
                        )
                    }
                }

                // ==========================================
                // 11. HAKKINDA
                // ==========================================
                item {
                    SettingsAccordionCard(
                        title = "Hakkında",
                        icon = Icons.Default.Info,
                        isExpanded = expandedSection == "hakkinda",
                        onToggle = { toggle("hakkinda") }
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Tivions Android IPTV Player",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sürüm 1.0.0 (Build 100)",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        ParentalPinDialog(
            isOpen = showPinDialog,
            title = "Ebeveyn Kilidi Ayarla",
            onDismiss = { showPinDialog = false },
            onSuccess = {
                showPinDialog = false
                onUpdateSettings(settings.copy(isParentalControlEnabled = true))
            }
        )
    }
}

@Composable
fun SettingsAccordionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    TivionsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsRowText(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = AccentCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsRowSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04101A),
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = Color(0xFF262C47)
            )
        )
    }
}
