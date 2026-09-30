package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LiveStreamPlayerViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun liveStreamPlayerView_rendersAndControlsInteractable() {
        var isFullscreenState = false
        var backClicked = false

        composeTestRule.setContent {
            LiveStreamPlayerView(
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                channelName = "TRT 1 HD",
                epgTitle = "Ana Haber Bülteni (20:00 - 21:00)",
                autoPlay = false,
                isFullscreen = isFullscreenState,
                showBackButton = true,
                onFullscreenToggle = { isFullscreenState = it },
                onBackClick = { backClicked = true }
            )
        }

        // Verify container is displayed
        composeTestRule.onNodeWithTag("live_stream_player_container").assertIsDisplayed()

        // Verify play/pause button is present and clickable
        composeTestRule.onNodeWithTag("btn_player_play_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_play_pause").performClick()

        // Verify fullscreen button is present and clickable
        composeTestRule.onNodeWithTag("btn_player_fullscreen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_fullscreen").performClick()
        assertTrue(isFullscreenState)

        // Verify back button is present and clickable
        composeTestRule.onNodeWithTag("btn_player_back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_player_back").performClick()
        assertTrue(backClicked)
    }
}
