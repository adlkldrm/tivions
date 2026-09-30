package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TivionsPrimaryButton
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundAppBrush
import com.example.ui.theme.BgCardDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ConnectSourceScreen(
    isLoading: Boolean,
    statusMessage: String?,
    isOnline: Boolean = true,
    onConnectM3u: (name: String, url: String, remember: Boolean) -> Unit,
    onConnectXtream: (name: String, serverUrl: String, user: String, pass: String, remember: Boolean) -> Unit,
    onLoadDemo: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: M3U, 1: Xtream

    // Double-click protection (throttle/debounce)
    var lastClickTimestamp by remember { mutableStateOf(0L) }
    fun canPerformAction(): Boolean {
        val now = System.currentTimeMillis()
        if (isLoading || (now - lastClickTimestamp < 1200L)) return false
        lastClickTimestamp = now
        return true
    }

    // M3U form state
    var playlistName by remember { mutableStateOf("Ev Listem") }
    var m3uUrl by remember { mutableStateOf("") }
    var rememberPlaylist by remember { mutableStateOf(true) }

    // Xtream form state
    var xtreamName by remember { mutableStateOf("Xtream Hesabım") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberXtream by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppBrush)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo & Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(16.dp, shape = RoundedCornerShape(26.dp), spotColor = AccentCyan)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF1E243D))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tivions",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tivions",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Yayın kaynağını bağla ve izlemeye başla",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Offline network warning
            AnimatedVisibility(visible = !isOnline) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "İnternet bağlantısı tespit edilemedi. Yayın kaynağına bağlanabilmek için lütfen ağ bağlantınızı kontrol edin.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Tab Selector (M3U Link vs Xtream Codes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCardDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // M3U Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 0) Color(0xFF2C324D) else Color.Transparent
                        )
                        .clickable { selectedTab = 0 }
                        .testTag("tab_m3u"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M3U Link",
                        color = if (selectedTab == 0) TextPrimary else TextTertiary,
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Xtream Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedTab == 1) Color(0xFF2C324D) else Color.Transparent
                        )
                        .clickable { selectedTab = 1 }
                        .testTag("tab_xtream"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Xtream Codes",
                        color = if (selectedTab == 1) TextPrimary else TextTertiary,
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (selectedTab == 0) {
                // ================= M3U LINK TAB =================
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Playlist adı",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = { Text("Ev Listem", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF191E33),
                            unfocusedContainerColor = Color(0xFF191E33),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_playlist_name")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "M3U URL",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        placeholder = { Text("http://sunucu-adresi.com/playlist.m3u", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF191E33),
                            unfocusedContainerColor = Color(0xFF191E33),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_m3u_url")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberPlaylist = !rememberPlaylist },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberPlaylist,
                            onCheckedChange = { rememberPlaylist = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentCyan,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = Color(0xFF0C0F1D)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bu listeyi hatırla",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TivionsPrimaryButton(
                        text = "Playlist'i bağla",
                        enabled = !isLoading && m3uUrl.isNotBlank(),
                        onClick = {
                            if (canPerformAction()) {
                                onConnectM3u(playlistName, m3uUrl.trim(), rememberPlaylist)
                            }
                        }
                    )
                }
            } else {
                // ================= XTREAM CODES TAB =================
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sunucu URL",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("http://domain.xyz:8080", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF191E33),
                            unfocusedContainerColor = Color(0xFF191E33),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_xtream_url")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kullanıcı Adı",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text("Kullanıcı adınız", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF191E33),
                            unfocusedContainerColor = Color(0xFF191E33),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_xtream_user")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Şifre",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = TextTertiary) },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF191E33),
                            unfocusedContainerColor = Color(0xFF191E33),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_xtream_pass")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberXtream = !rememberXtream },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberXtream,
                            onCheckedChange = { rememberXtream = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentCyan,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = Color(0xFF0C0F1D)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bu hesabı hatırla",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TivionsPrimaryButton(
                        text = "Giriş yap ve Bağla",
                        enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank(),
                        onClick = {
                            if (canPerformAction()) {
                                onConnectXtream(xtreamName, serverUrl.trim(), username.trim(), password.trim(), rememberXtream)
                            }
                        }
                    )
                }
            }

            // Status feedback / Loading
            AnimatedVisibility(visible = isLoading) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage ?: "İçerikler yükleniyor...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            if (!isLoading && statusMessage != null) {
                val isError = statusMessage.startsWith("Hata")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isError) Color(0x26EF4444) else Color(0x2606B6D4))
                        .border(1.dp, if (isError) Color(0xFFEF4444).copy(alpha = 0.5f) else AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) Color(0xFFEF4444) else AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = statusMessage.removePrefix("Hata: "),
                            color = if (isError) Color(0xFFFCA5A5) else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (onBack != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) {
                    Text("Vazgeç ve Geri Dön", color = TextTertiary)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
