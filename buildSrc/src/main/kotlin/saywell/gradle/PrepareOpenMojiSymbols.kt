package saywell.gradle

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Turns the OpenMoji npm package into app assets:
 *
 * - `symbols/openmoji/<CODE>.svg` for every colour emoji without a skin-tone variant
 *   (skin tones can be chosen as separate symbols later; this keeps the APK small),
 * - `symbols/openmoji/index.json`, the search index read by `SymbolSearch`,
 * - `symbols/openmoji/LICENSE.txt` (CC BY-SA 4.0).
 *
 * The app's unit tests check that every symbol the starter vocabulary uses is in the output.
 */
@CacheableTask
abstract class PrepareOpenMojiSymbols : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: ConfigurableFileCollection

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun prepare() {
        val tgz = archive.singleFile
        verifySha256(tgz, expectedSha256.get(), "OpenMoji package")

        val unpacked = File(temporaryDir, "openmoji")
        fileSystem.delete { delete(unpacked) }
        fileSystem.copy {
            from(archives.tarTree(archives.gzip(tgz)))
            include("package/data/openmoji.json", "package/LICENSE.txt", "package/color/svg/*.svg")
            into(unpacked)
        }

        val out = outputDirectory.get().asFile
        fileSystem.delete { delete(out) }
        val symbolsDir = File(out, "symbols/openmoji").apply { mkdirs() }

        @Suppress("UNCHECKED_CAST")
        val entries = JsonSlurper().parse(File(unpacked, "package/data/openmoji.json")) as List<Map<String, Any?>>
        val svgDir = File(unpacked, "package/color/svg")
        val index = mutableListOf<Map<String, String>>()
        for (entry in entries) {
            val code = entry["hexcode"] as? String ?: continue
            val group = entry["group"] as? String ?: ""
            if (entry["skintone"]?.toString().orEmpty().isNotEmpty() || group == "component") continue
            val svg = File(svgDir, "$code.svg")
            if (!svg.isFile) continue
            File(symbolsDir, "$code.svg").writeText(minifySvg(svg.readText()))
            val keywords = listOf(entry["tags"] as? String, entry["openmoji_tags"] as? String)
                .flatMap { it.orEmpty().split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            index += linkedMapOf(
                "c" to code,
                "n" to (entry["annotation"] as? String).orEmpty(),
                "k" to keywords,
                "g" to group,
            )
        }
        File(symbolsDir, "index.json").writeText(JsonOutput.toJson(index))
        File(unpacked, "package/LICENSE.txt").copyTo(File(symbolsDir, "LICENSE.txt"), overwrite = true)

        logger.lifecycle("OpenMoji: bundled ${index.size} symbols")
    }

    private fun minifySvg(svg: String): String = svg
        .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""<g id="[^"]*"\s*/>"""), "")
        .replace(Regex(""">\s+<"""), "><")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
}
