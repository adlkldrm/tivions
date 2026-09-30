package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpgProgram
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Reusable Compose component that presents current and upcoming Electronic Program Guide (EPG)
 * information for a live TV channel.
 *
 * Displays:
 * - Real-time "CANLI" status badge.
 * - Currently airing program with progress bar and time range.
 * - Upcoming programs list with starting time, title, and category.
 * - Optional reminder toggle callback.
 */
@Composable
fun ChannelEpgInfoView(
    programs: List<EpgProgram>,
    modifier: Modifier = Modifier,
    maxUpcoming: Int = 3,
    currentTime: Long = System.currentTimeMillis(),
    showUpcomingList: Boolean = true,
    onProgramClick: ((EpgProgram) -> Unit)? = null,
    onToggleReminder: ((EpgProgram) -> Unit)? = null
) {
    val currentProgram = remember(programs, currentTime) {
        programs.firstOrNull { it.isCurrentlyPlaying(currentTime) }
    }
    val upcomingPrograms = remember(programs, currentTime, maxUpcoming) {
        programs.filter { it.isUpcoming(currentTime) }
            .sortedBy { it.startTimeMillis }
            .take(maxUpcoming)
    }

    if (currentProgram == null && upcomingPrograms.isEmpty()) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x330B132B))
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("channel_epg_info_view")
    ) {
        // Current Program Section
        if (currentProgram != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onProgramClick != null) Modifier.clickable { onProgramClick(currentProgram) }
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glowing CANLI pulse tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DangerRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CANLI",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = currentProgram.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = currentProgram.formattedTimeRange(),
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            val progress = currentProgram.progressFraction(currentTime)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentCyan,
                trackColor = Color(0x33475569),
                strokeCap = StrokeCap.Round
            )
        }

        // Upcoming Programs Section
        if (showUpcomingList && upcomingPrograms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sonraki Programlar",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                upcomingPrograms.forEach { prog ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x221E243A))
                            .then(
                                if (onProgramClick != null) Modifier.clickable { onProgramClick(prog) }
                                else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prog.formattedStartTime(),
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = prog.title,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prog.category,
                            color = TextTertiary,
                            fontSize = 10.sp
                        )

                        if (onToggleReminder != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Hatırlatıcı Ayarla",
                                tint = AccentCyan,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onToggleReminder(prog) }
                            )
                        }
                    }
                }
            }
        }
    }
}
