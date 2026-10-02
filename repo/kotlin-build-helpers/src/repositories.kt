/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.githubTag(ghUser: String, repo: String, revisionPrefix: String = "v", groupAlias: String? = null) {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Github Tag: $ghUser/$repo"
                setUrl("https://github.com/$ghUser/$repo/archive/refs/tags/")
                patternLayout {
                    artifact("$revisionPrefix[revision].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.githubRelease(ghUser: String, repo: String, revisionPrefix: String = "v", groupAlias: String? = null) {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Github Release: $ghUser/$repo"
                setUrl("https://github.com/$ghUser/$repo/releases/download/")
                patternLayout {
                    artifact("$revisionPrefix[revision]/[artifact](-$revisionPrefix[revision])(-[classifier]).[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.githubCommit(ghUser: String, repo: String, groupAlias: String? = null) {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Github Commit: $ghUser/$repo"
                setUrl("https://github.com/$ghUser/$repo/archive/")
                patternLayout {
                    artifact("[revision].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.kotlinDependencies() {
    exclusiveContent {
        forRepository {
            maven {
                name = "kotlin-dependencies"
                setUrl("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            }
        }
        filter {
            includeModule("org.jetbrains.dukat", "dukat")
            includeModule("org.jetbrains.kotlin", "android-dx")
            includeModule("org.jetbrains.kotlin", "jcabi-aether")
            includeModule("org.jetbrains.kotlin", "protobuf-lite")
            includeModule("org.jetbrains.kotlin", "protobuf-relocated")
            includeModule("org.jetbrains.kotlinx", "kotlinx-metadata-klib")
        }
    }
}

fun RepositoryHandler.intellijRepository(intellijSdkVersion: String) {
    exclusiveContent {
        forRepository {
            val isEAPIntellij = intellijSdkVersion.contains("-EAP-")
            val isNightlyIntellij = intellijSdkVersion.endsWith("SNAPSHOT") && !isEAPIntellij
            val intellijRepo =
                when {
                    isEAPIntellij -> "https://www.jetbrains.com/intellij-repository/snapshots"
                    isNightlyIntellij -> "https://www.jetbrains.com/intellij-repository/nightly"
                    else -> "https://www.jetbrains.com/intellij-repository/releases"
                }

            maven {
                name = "intellij-repository"
                setUrl(intellijRepo)
            }
        }
        filter {
            includeGroupByRegex("com\\.jetbrains\\.intellij(\\..+)?")
        }
    }
}

fun RepositoryHandler.intellijDependencies() {
    exclusiveContent {
        forRepository {
            maven {
                name = "intellij-dependencies"
                setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            }
        }
        filter {
            includeModule("org.jetbrains.intellij.deps", "jdom")
            includeModule("org.jetbrains.intellij.deps", "log4j")
            includeModule("org.jetbrains.intellij.deps", "asm-all")
            includeModule("org.jetbrains.intellij.deps", "gradle-api")
            includeModule("org.jetbrains.intellij.deps", "rwmutex-idea")
            includeGroupByRegex("org.jetbrains.intellij.deps.jflex.*")
            includeGroupByRegex("org.jetbrains.intellij.deps.android.tools.*")
            includeGroupByRegex("org.jetbrains.intellij.deps.fastutil.*")
            includeGroupByRegex("org.jetbrains.intellij.deps.jna.*")
            includeGroupByRegex("com.intellij.platform.*")
            includeGroupByRegex("org.jetbrains.jps.*")
            includeVersion("org.jetbrains.jps", "jps-javac-extension", "7")
            includeVersion("com.google.protobuf", "protobuf-parent", "3.24.4-jb.2")
            includeVersion("com.google.protobuf", "protobuf-java", "3.24.4-jb.2")
            includeVersion("com.google.protobuf", "protobuf-bom", "3.24.4-jb.2")
            includeModuleByRegex("org\\.jetbrains", "(syntax\\-api|lang\\-syntax|multiplatform).*")
        }
    }
}

fun RepositoryHandler.teamcityRepository() {
    exclusiveContent {
        forRepository {
            maven {
                name = "teamcity-repository"
                setUrl("https://download.jetbrains.com/teamcity-repository")
            }
        }
        filter {
            includeModule("org.jetbrains.teamcity", "serviceMessages")
            includeModule("org.jetbrains.teamcity.idea", "annotations")
        }
    }
}

fun RepositoryHandler.androidxSnapshotRepository(composeSnapshotId: String) {
    maven {
        setUrl("https://androidx.dev/snapshots/builds/$composeSnapshotId/artifacts/repository")
        content {
            includeGroup("androidx.compose.runtime")
            includeGroup("androidx.collection")
            includeGroup("androidx.annotation")
        }
    }
}

fun RepositoryHandler.googleAndroidRepository() {
    exclusiveContent {
        forRepository {
            maven {
                url = google().url
                content {
                    includeGroupByRegex("""androidx(\..*)?""")
                }
            }
        }
        filter {
            includeGroupByRegex("""com\.android(\..*)?""")
            includeGroup("com.google.testing.platform")
        }
    }
}

fun RepositoryHandler.gradleLibsReleases() {
    exclusiveContent {
        forRepository {
            maven {
                name = "Gradle Libs Releases"
                setUrl("https://repo.gradle.org/gradle/libs-releases")
            }
        }
        filter {
            includeGroup("org.gradle.experimental")
        }
    }
}

fun RepositoryHandler.gradlePluginPortalRepository() {
    exclusiveContent {
        forRepository {
            gradlePluginPortal()
        }
        filter {
            includeGroup("com.gradle")
        }
    }
}

fun RepositoryHandler.litmuskt() {
    exclusiveContent {
        forRepository {
            maven {
                name = "litmuskt"
                setUrl("https://packages.jetbrains.team/maven/p/plan/litmuskt")
            }
        }
        filter {
            includeGroupByRegex("org\\.jetbrains\\.litmuskt(\\..+)?")
        }
    }
}

fun RepositoryHandler.kotlinIdePluginDependencies() {
    exclusiveContent {
        forRepository {
            maven {
                name = "kotlin-ide-plugin-dependencies"
                setUrl("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
            }
        }
        filter {
            val kotlinGradlePluginIdeaTestedVersion = "1.8.20-dev-4242"
            includeModule("org.jetbrains.kotlin", "kotlin-gradle-plugin-idea")
            includeModule("org.jetbrains.kotlin", "kotlin-gradle-plugin-idea-proto")
            includeVersionByRegex("org.jetbrains.kotlin", ".*", kotlinGradlePluginIdeaTestedVersion)
        }
    }
}

fun RepositoryHandler.mozillaReleases() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Mozilla Releases"
                setUrl("https://archive.mozilla.org/pub/firefox/releases/")
                patternLayout {
                    artifact("[revision]/jsshell/[artifact]-[classifier].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("org.mozilla", "jsshell")
        }
    }
}

fun RepositoryHandler.kotlinFileDependenciesJsc() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "kotlin-file-dependencies-jsc"
                setUrl("https://packages.jetbrains.team/files/p/kt/kotlin-file-dependencies/javascriptcore/")
                patternLayout {
                    artifact("[classifier]_[revision].zip")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("org.jsc", "jsc")
        }
    }
}

fun RepositoryHandler.nodeJs() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Node.js"
                setUrl("https://cache-redirector.jetbrains.com/nodejs.org/dist")
                patternLayout {
                    artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("org.nodejs", "node")
        }
    }
}

fun RepositoryHandler.yarnDistributions() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Yarn Distributions"
                setUrl("https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download")
                patternLayout {
                    artifact("v[revision]/[artifact](-v[revision]).[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("com.yarnpkg", "yarn")
        }
    }
}

fun RepositoryHandler.binaryenDistributions() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Binaryen Distributions"
                setUrl("https://cache-redirector.jetbrains.com/github.com/WebAssembly/binaryen/releases/download")
                patternLayout {
                    artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("com.github.webassembly", "binaryen")
        }
    }
}

fun RepositoryHandler.d8Distributions() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "D8 Distributions"
                setUrl("https://cache-redirector.jetbrains.com/storage.googleapis.com/chromium-v8/official/canary")
                patternLayout {
                    artifact("[artifact]-[revision].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("google.d8", "v8")
        }
    }
}

fun RepositoryHandler.wasmtimeDistributions() {
    exclusiveContent {
        forRepository {
            ivy {
                name = "Wasmtime Distributions"
                setUrl("https://cache-redirector.jetbrains.com/github.com/bytecodealliance/wasmtime/releases/download")
                patternLayout {
                    artifact("v[revision]/[artifact]-v[revision]-[classifier].[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("bytecodealliance.wasmtime", "wasmtime")
        }
    }
}

fun RepositoryHandler.androidRepository() {
    exclusiveContent {
        forRepository {
            ivy {
                setUrl("https://dl.google.com/android/repository")
                patternLayout {
                    artifact("[artifact]-[revision].[ext]")
                    artifact("[artifact]_[revision](-[classifier]).[ext]")
                    artifact("[artifact]_[revision](_[classifier]).[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("google", "platform-tools")
            includeModule("google", "commandlinetools-linux")
            includeModule("google", "commandlinetools-win")
            includeModule("google", "commandlinetools-mac")
            includeModule("google", "emulator-linux_x64")
            includeModule("google", "emulator-windows_x64")
            includeModule("google", "emulator-darwin_aarch64")
            includeModule("google", "android")
            includeModule("google", "platform")
            includeModule("google", "android_m2repository")
            includeModule("google", "build-tools")
            includeModuleByRegex("google", """.*\.build-tools""")
        }
    }
}

fun RepositoryHandler.androidSystemImages() {
    exclusiveContent {
        forRepository {
            ivy {
                setUrl("https://dl.google.com/android/repository/sys-img/android")
                patternLayout {
                    artifact("[artifact]-[revision](_[classifier]).[ext]")
                }
                metadataSources { artifact() }
            }
        }
        filter {
            includeModule("google", "arm64-v8a")
            includeModule("google", "x86_64")
        }
    }
}
