package io.github.lobojoel2003.saywell.core.obf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/*
 * Open Board Format (OBF) 0.1 data model: https://www.openboardformat.org/docs
 *
 * Files from other AAC apps are not always strict (numeric ids, fractional sizes, missing fields),
 * so reading is lenient. Writing follows the spec.
 */

const val OBF_FORMAT = "open-board-0.1"

@Serializable
data class ObfBoard(
    val format: String = OBF_FORMAT,
    @Serializable(with = LenientString::class) val id: String,
    val locale: String? = null,
    val name: String? = null,
    @SerialName("description_html") val descriptionHtml: String? = null,
    val url: String? = null,
    val buttons: List<ObfButton> = emptyList(),
    val grid: ObfGrid? = null,
    val images: List<ObfImage> = emptyList(),
    val sounds: List<ObfSound> = emptyList(),
    val license: ObfLicense? = null,
)

@Serializable
data class ObfButton(
    @Serializable(with = LenientString::class) val id: String,
    val label: String? = null,
    val vocalization: String? = null,
    @SerialName("image_id") @Serializable(with = LenientString::class) val imageId: String? = null,
    @SerialName("sound_id") @Serializable(with = LenientString::class) val soundId: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("load_board") val loadBoard: ObfLoadBoard? = null,
    val action: String? = null,
    val actions: List<String>? = null,
    val hidden: Boolean? = null,
)

@Serializable
data class ObfLoadBoard(
    @Serializable(with = LenientString::class) val id: String? = null,
    val name: String? = null,
    @SerialName("data_url") val dataUrl: String? = null,
    val url: String? = null,
    val path: String? = null,
)

@Serializable
data class ObfGrid(
    @Serializable(with = LenientInt::class) val rows: Int,
    @Serializable(with = LenientInt::class) val columns: Int,
    val order: List<List<@Serializable(with = LenientString::class) String?>> = emptyList(),
)

@Serializable
data class ObfImage(
    @Serializable(with = LenientString::class) val id: String,
    @Serializable(with = LenientInt::class) val width: Int? = null,
    @Serializable(with = LenientInt::class) val height: Int? = null,
    @SerialName("content_type") val contentType: String? = null,
    val data: String? = null,
    val url: String? = null,
    @SerialName("data_url") val dataUrl: String? = null,
    val path: String? = null,
    val symbol: ObfSymbol? = null,
    val license: ObfLicense? = null,
)

@Serializable
data class ObfSymbol(val set: String? = null, val filename: String? = null)

@Serializable
data class ObfSound(
    @Serializable(with = LenientString::class) val id: String,
    @SerialName("content_type") val contentType: String? = null,
    val data: String? = null,
    val url: String? = null,
    val path: String? = null,
)

@Serializable
data class ObfLicense(
    val type: String? = null,
    @SerialName("copyright_notice_url") val copyrightNoticeUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_url") val authorUrl: String? = null,
)

/** `manifest.json` inside an `.obz` package. */
@Serializable
data class ObfManifest(
    val format: String = OBF_FORMAT,
    val root: String,
    val paths: ObfPaths = ObfPaths(),
)

@Serializable
data class ObfPaths(
    val boards: Map<String, String> = emptyMap(),
    val images: Map<String, String> = emptyMap(),
    val sounds: Map<String, String> = emptyMap(),
)

/** JSON settings for OBF: lenient when reading, compact and spec-shaped when writing. */
val ObfJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
}

/** Accepts `"12"` or `12` for ids, as files in the wild use both. */
internal object LenientString : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("ObfLenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = json.decodeJsonElement()
        return (element as? JsonPrimitive)?.content ?: throw SerializationException("Expected a string or number, got $element")
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/** Accepts `3`, `3.0` or `"3"` for counts and sizes. */
internal object LenientInt : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("ObfLenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val json = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = json.decodeJsonElement()
        val text = (element as? JsonPrimitive)?.content ?: throw SerializationException("Expected a number, got $element")
        return text.toDoubleOrNull()?.toInt() ?: throw SerializationException("Expected a number, got $text")
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}
