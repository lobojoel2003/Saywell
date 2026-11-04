pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()

        // Sherpa-ONNX publishes its Android library (AAR) only as a GitHub release asset:
        // https://github.com/k2-fsa/sherpa-onnx/releases/download/v<version>/sherpa-onnx-<version>.aar
        exclusiveContent {
            forRepository {
                ivy("https://github.com/k2-fsa/sherpa-onnx/releases/download") {
                    name = "sherpaOnnxReleases"
                    patternLayout { artifact("v[revision]/[module]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("com.k2fsa", "sherpa-onnx") }
        }

        // Piper voices repackaged for Sherpa-ONNX (model, tokens and espeak-ng data):
        // https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<voice>.tar.bz2
        exclusiveContent {
            forRepository {
                ivy("https://github.com/k2-fsa/sherpa-onnx/releases/download") {
                    name = "sherpaOnnxVoices"
                    patternLayout { artifact("[revision]/[module].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("com.k2fsa.voices") }
        }

        // OpenMoji symbols, published as an npm package.
        exclusiveContent {
            forRepository {
                ivy("https://registry.npmjs.org") {
                    name = "npmRegistry"
                    patternLayout { artifact("[module]/-/[module]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("npm") }
        }
    }
}

rootProject.name = "Saywell"

include(":app", ":core")
