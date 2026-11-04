package saywell.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
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
 * Unpacks a Sherpa-ONNX Piper voice package (`vits-piper-<voice>.tar.bz2`) into app assets:
 *
 * - `voices/bundled/<voice>/model.onnx`, `tokens.txt` and `MODEL_CARD` (the voice's licence),
 * - `espeak-ng-data/`, which turns text into phonemes. Only the dictionaries in [keepDictionaries]
 *   are kept; voices for other languages bring their own when they are installed.
 */
@CacheableTask
abstract class PrepareBundledVoice : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: ConfigurableFileCollection

    @get:Input
    abstract val expectedSha256: Property<String>

    /** The package name, for example `vits-piper-en_US-kristin-medium`. */
    @get:Input
    abstract val voicePackage: Property<String>

    /** espeak-ng dictionaries to keep, for example `en_dict`. */
    @get:Input
    abstract val keepDictionaries: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun prepare() {
        val tarball = archive.singleFile
        verifySha256(tarball, expectedSha256.get(), "Voice package ${voicePackage.get()}")

        val unpacked = File(temporaryDir, "voice")
        fileSystem.delete { delete(unpacked) }
        fileSystem.copy {
            from(archives.tarTree(archives.bzip2(tarball)))
            into(unpacked)
        }
        val root = unpacked.listFiles()?.singleOrNull { it.isDirectory } ?: unpacked
        val model = root.listFiles()?.filter { it.isFile && it.name.endsWith(".onnx") }?.singleOrNull()
            ?: throw GradleException("${tarball.name} should contain exactly one .onnx model")
        val tokens = File(root, "tokens.txt").takeIf { it.isFile }
            ?: throw GradleException("${tarball.name} has no tokens.txt")
        val espeak = File(root, "espeak-ng-data").takeIf { it.isDirectory }
            ?: throw GradleException("${tarball.name} has no espeak-ng-data folder")

        val voiceId = voicePackage.get().removePrefix("vits-piper-")
        val out = outputDirectory.get().asFile
        fileSystem.delete { delete(out) }
        val voiceDir = File(out, "voices/bundled/$voiceId").apply { mkdirs() }
        model.copyTo(File(voiceDir, "model.onnx"))
        tokens.copyTo(File(voiceDir, "tokens.txt"))
        val card = File(root, "MODEL_CARD")
        if (card.isFile) {
            card.copyTo(File(voiceDir, "MODEL_CARD"))
            card.readLines().filter { it.contains("license", ignoreCase = true) || it.contains("dataset", ignoreCase = true) }
                .forEach { logger.lifecycle("Voice $voiceId: ${it.trim()}") }
        } else {
            logger.warn("Voice $voiceId has no MODEL_CARD; check its licence before releasing")
        }

        val keep = keepDictionaries.get().toSet()
        fileSystem.copy {
            from(espeak)
            exclude { details -> !details.isDirectory && details.name.endsWith("_dict") && details.name !in keep }
            into(File(out, "espeak-ng-data"))
        }
        val bytes = out.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        logger.lifecycle("Voice $voiceId: bundled ${bytes / (1024 * 1024)} MB")
    }
}
