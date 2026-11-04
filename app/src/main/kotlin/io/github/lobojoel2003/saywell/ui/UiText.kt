package io.github.lobojoel2003.saywell.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text for a snackbar or dialog: a string resource, or text that is already in words (an error message). */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Raw(val text: String) : UiText

    fun resolve(resources: Resources): String = when (this) {
        is Res -> resources.getString(id, *args.toTypedArray())
        is Raw -> text
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> text
}
