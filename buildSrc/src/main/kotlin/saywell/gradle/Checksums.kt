package saywell.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

internal fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** Fails the build if [file] doesn't match the pinned checksum, printing the actual one. */
internal fun verifySha256(file: File, expected: String, what: String) {
    val actual = sha256(file)
    if (!actual.equals(expected.trim(), ignoreCase = true)) {
        throw GradleException(
            """
            |$what does not match its pinned SHA-256 checksum.
            |  file:     ${file.name}
            |  expected: $expected
            |  actual:   $actual
            |If you changed the version on purpose, check the new file and put its checksum in gradle/libs.versions.toml.
            """.trimMargin(),
        )
    }
}

/** Checks a downloaded dependency (for example the Sherpa-ONNX AAR) against its pinned SHA-256. */
@CacheableTask
abstract class VerifySha256 : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifact: ConfigurableFileCollection

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:Input
    abstract val label: Property<String>

    @get:OutputFile
    abstract val receipt: RegularFileProperty

    @TaskAction
    fun verify() {
        val file = artifact.singleFile
        verifySha256(file, expectedSha256.get(), label.get())
        receipt.get().asFile.writeText("${file.name} ${expectedSha256.get()}\n")
    }
}
