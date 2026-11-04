package io.github.lobojoel2003.saywell.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.core.model.ImageRef

/** Draws a button's picture: a bundled symbol or a saved photo. Decorative; the label carries the meaning. */
@Composable
fun SymbolImage(image: ImageRef, modifier: Modifier = Modifier) {
    val store = LocalContext.current.container.images
    AsyncImage(
        model = store.model(image),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** A bundled OpenMoji symbol by code. */
@Composable
fun OpenMojiImage(code: String, modifier: Modifier = Modifier) = SymbolImage(ImageRef.openMoji(code), modifier)
