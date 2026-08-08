package com.vocalingo.app

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule(order = 0)
    val recordAudioPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreenShowsSplitConversationControls() {
        composeRule.onNodeWithText("VocaLingo").assertIsDisplayed()
        composeRule.onNodeWithTag("top-status-bar").assertIsDisplayed()
        composeRule.onNodeWithTag("user-1-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("user-2-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("user-1-mic-button").assertIsDisplayed()
        composeRule.onNodeWithTag("user-2-mic-button").assertIsDisplayed()
        composeRule.onNodeWithText("Test").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun earTestUpdatesCalibrationStatus() {
        composeRule.onNodeWithTag("ear-test-button").performClick()
        composeRule.onNodeWithText("Stereo works").assertIsDisplayed()
        composeRule.onNodeWithTag("ear-test-pass-button").performClick()

        composeRule
            .onNodeWithTag("conversation-status")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ear test passed.").assertIsDisplayed()
    }
}
