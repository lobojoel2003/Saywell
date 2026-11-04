package io.github.lobojoel2003.saywell

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.lobojoel2003.saywell.ui.SaywellApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SaywellApp() }
    }

    /**
     * Switch interfaces arrive as key presses; they go to scanning before anything else sees them.
     * Overriding Activity.dispatchKeyEvent is public API; lint flags it only because androidx's
     * ComponentActivity redeclares it as library-internal.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val app = container
        if (app.switches.onKeyEvent(event, app.settings.settings.value)) return true
        return super.dispatchKeyEvent(event)
    }
}
