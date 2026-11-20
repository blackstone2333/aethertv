package app.orchard.tv.ui.components

import android.content.ComponentName
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.apps.TileArt
import app.orchard.tv.ui.home.AppTile
import app.orchard.tv.ui.theme.LocalReducedMotion
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class AccessibilityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var host: ActivityController<ComponentActivity>? = null
    private lateinit var view: View

    @After fun close() { compose.runOnUiThread { host?.close() } }

    private fun mount(content: @Composable () -> Unit) {
        compose.runOnUiThread {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java)
            host = controller
            controller.setup()
            controller.get().setContent {
                view = LocalView.current
                CompositionLocalProvider(LocalReducedMotion provides true, content = content)
            }
            controller.windowFocusChanged(true)
        }
        compose.waitForIdle()
    }

    @Test fun pressableMergesVisibleTextIntoOneAccessibleButton() {
        var clicks = 0
        mount { Pressable({ clicks++ }) { BasicText("Settings") } }
        compose.onNodeWithText("Settings")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test fun glyphLabelUsesTheLatestCallback() {
        var first = 0
        var second = 0
        var changed by mutableStateOf(false)
        mount { Pressable(if (changed) ({ second++ }) else ({ first++ }), label = "Control Center") {} }
        val button = compose.onNodeWithContentDescription("Control Center")
        button.performClick()
        compose.runOnUiThread { changed = true }
        button.performClick()
        assertEquals(1, first)
        assertEquals(1, second)
    }

    @Test fun disabledControlRejectsEvenDirectAccessibilityInvocation() {
        var clicks = 0
        mount { Pressable({ clicks++ }, enabled = false, label = "Unavailable") {} }
        compose.onNodeWithContentDescription("Unavailable").assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { assertFalse(it()) }
        assertEquals(0, clicks)
    }

    @Test fun remoteReleaseMustStillBelongToThisControl() {
        var clicks = 0
        val focus = FocusRequester()
        mount { Pressable({ clicks++ }, focusRequester = focus, label = "Open") { BasicText("Open") } }
        compose.runOnUiThread {
            focus.requestFocus()
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
        assertEquals(0, clicks)
        compose.runOnUiThread {
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
        assertEquals(1, clicks)
    }

    @Test fun namelessDockTileExposesItsAppAndBothActions() {
        var opens = 0
        var menus = 0
        mount { tile(true, { opens++ }, { menus++ }) }
        compose.onNodeWithContentDescription("A test app").performClick()
            .performSemanticsAction(SemanticsActions.OnLongClick) { assertTrue(it()) }
        assertEquals(1, opens)
        assertEquals(1, menus)
    }

    @Test fun disabledTileRejectsOpenAndMenuAccessibilityActions() {
        var actions = 0
        mount { tile(false, { actions++ }, { actions++ }) }
        compose.onNodeWithContentDescription("A test app").assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { assertFalse(it()) }
            .performSemanticsAction(SemanticsActions.OnLongClick) { assertFalse(it()) }
        assertEquals(0, actions)
    }

    @Composable private fun tile(enabled: Boolean, open: () -> Unit, menu: () -> Unit) {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
        val art = TileArt(bitmap, null, Rect.Zero, Color.Black, bitmap)
        AppTile(AppEntry("test.app", "A test app", ComponentName("test.app", "Main"), true, true, art),
            140.dp, 80.dp, FocusRequester(), enabled, false, null, {}, { open() }, { menu() },
            showName = false, rasterize = false)
    }
}
