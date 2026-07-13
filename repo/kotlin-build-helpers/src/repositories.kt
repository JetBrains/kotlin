/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.githubTag(ghUser: String, repo: String, revisionPrefix: String = "v", groupAlias: String? = null) {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Github Tag: $ghUser/$repo"
                repository.setUrl("https://github.com/$ghUser/$repo/archive/refs/tags/")
                repository.patternLayout { layout ->
                    layout.artifact("$revisionPrefix[revision].[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.githubRelease(ghUser: String, repo: String, revisionPrefix: String = "v", groupAlias: String? = null) {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Github Release: $ghUser/$repo"
                repository.setUrl("https://github.com/$ghUser/$repo/releases/download/")
                repository.patternLayout { layout ->
                    layout.artifact("$revisionPrefix[revision]/[artifact](-$revisionPrefix[revision])(-[classifier]).[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.githubCommit(ghUser: String, repo: String, groupAlias: String? = null) {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Github Commit: $ghUser/$repo"
                repository.setUrl("https://github.com/$ghUser/$repo/archive/")
                repository.patternLayout { layout ->
                    layout.artifact("[revision].[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule(groupAlias ?: ghUser, repo)
        }
    }
}

fun RepositoryHandler.kotlinDependencies() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "kotlin-dependencies"
                repository.setUrl("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            }
        }
        exclusive.filter { content ->
            content.includeModule("org.jetbrains.dukat", "dukat")
            content.includeModule("org.jetbrains.kotlin", "android-dx")
            content.includeModule("org.jetbrains.kotlin", "jcabi-aether")
            content.includeModule("org.jetbrains.kotlin", "protobuf-lite")
            content.includeModule("org.jetbrains.kotlin", "protobuf-relocated")
            content.includeModule("org.jetbrains.kotlinx", "kotlinx-metadata-klib")
        }
    }
}

fun RepositoryHandler.intellijRepository(intellijSdkVersion: String) {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            val isEAPIntellij = intellijSdkVersion.contains("-EAP-")
            val isNightlyIntellij = intellijSdkVersion.endsWith("SNAPSHOT") && !isEAPIntellij
            val intellijRepo =
                when {
                    isEAPIntellij -> "https://www.jetbrains.com/intellij-repository/snapshots"
                    isNightlyIntellij -> "https://www.jetbrains.com/intellij-repository/nightly"
                    else -> "https://www.jetbrains.com/intellij-repository/releases"
                }

            maven { repository ->
                repository.name = "intellij-repository"
                repository.setUrl(intellijRepo)
            }
        }
        exclusive.filter { content ->
            content.includeGroupByRegex("com\\.jetbrains\\.intellij(\\..+)?")
        }
    }
}

fun RepositoryHandler.intellijDependencies() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "intellij-dependencies"
                repository.setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            }
        }
        exclusive.filter { content ->
            content.includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
            content.includeGroupByRegex("com.intellij.platform.*")
            content.includeGroupByRegex("org.jetbrains.jps.*")
            content.includeVersion("org.jetbrains.jps", "jps-javac-extension", "7")
            content.includeVersion("com.google.protobuf", "protobuf-parent", "3.24.4-jb.2")
            content.includeVersion("com.google.protobuf", "protobuf-java", "3.24.4-jb.2")
            content.includeVersion("com.google.protobuf", "protobuf-bom", "3.24.4-jb.2")
            content.includeModuleByRegex("org\\.jetbrains", "(syntax\\-api|lang\\-syntax).*")
        }
    }
}

fun RepositoryHandler.teamcityRepository() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "teamcity-repository"
                repository.setUrl("https://download.jetbrains.com/teamcity-repository")
            }
        }
        exclusive.filter { content ->
            content.includeModule("org.jetbrains.teamcity", "serviceMessages")
            content.includeModule("org.jetbrains.teamcity.idea", "annotations")
        }
    }
}

fun RepositoryHandler.googleAndroidRepository() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            google()
        }
        exclusive.filter { content ->
            content.includeGroupByRegex("""com\.android(\..*)?""")
            content.includeGroupByRegex("""androidx(\..*)?""")
            content.includeGroup("com.google.testing.platform")
        }
    }
}

fun RepositoryHandler.gradleLibsReleases() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "Gradle Libs Releases"
                repository.setUrl("https://repo.gradle.org/gradle/libs-releases")
            }
        }
        exclusive.filter { content ->
            content.includeGroup("org.gradle.experimental")
        }
    }
}

fun RepositoryHandler.gradlePluginPortalRepository() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            gradlePluginPortal()
        }
        exclusive.filter { content ->
            content.includeGroup("com.gradle")
        }
    }
}

fun RepositoryHandler.litmuskt() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "litmuskt"
                repository.setUrl("https://packages.jetbrains.team/maven/p/plan/litmuskt")
            }
        }
        exclusive.filter { content ->
            content.includeGroupByRegex("org\\.jetbrains\\.litmuskt(\\..+)?")
        }
    }
}

fun RepositoryHandler.kotlinIdePluginDependencies() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            maven { repository ->
                repository.name = "kotlin-ide-plugin-dependencies"
                repository.setUrl("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
            }
        }
        exclusive.filter { content ->
            val kotlinGradlePluginIdeaTestedVersion = "1.8.20-dev-4242"
            content.includeModule("org.jetbrains.kotlin", "kotlin-gradle-plugin-idea")
            content.includeModule("org.jetbrains.kotlin", "kotlin-gradle-plugin-idea-proto")
            content.includeVersionByRegex("org.jetbrains.kotlin", ".*", kotlinGradlePluginIdeaTestedVersion)
        }
    }
}

fun RepositoryHandler.mozillaReleases() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Mozilla Releases"
                repository.setUrl("https://archive.mozilla.org/pub/firefox/releases/")
                repository.patternLayout { layout ->
                    layout.artifact("[revision]/jsshell/[artifact]-[classifier].[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("org.mozilla", "jsshell")
        }
    }
}

fun RepositoryHandler.kotlinFileDependenciesJsc() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "kotlin-file-dependencies-jsc"
                repository.setUrl("https://packages.jetbrains.team/files/p/kt/kotlin-file-dependencies/javascriptcore/")
                repository.patternLayout { layout ->
                    layout.artifact("[classifier]_[revision].zip")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("org.jsc", "jsc")
        }
    }
}

fun RepositoryHandler.nodeJs() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Node.js"
                repository.setUrl("https://cache-redirector.jetbrains.com/nodejs.org/dist")
                repository.patternLayout { layout ->
                    layout.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("org.nodejs", "node")
        }
    }
}

fun RepositoryHandler.yarnDistributions() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Yarn Distributions"
                repository.setUrl("https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download")
                repository.patternLayout { layout ->
                    layout.artifact("v[revision]/[artifact](-v[revision]).[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("com.yarnpkg", "yarn")
        }
    }
}

fun RepositoryHandler.binaryenDistributions() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "Binaryen Distributions"
                repository.setUrl("https://cache-redirector.jetbrains.com/github.com/WebAssembly/binaryen/releases/download")
                repository.patternLayout { layout ->
                    layout.artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("com.github.webassembly", "binaryen")
        }
    }
}

fun RepositoryHandler.d8Distributions() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.name = "D8 Distributions"
                repository.setUrl("https://cache-redirector.jetbrains.com/storage.googleapis.com/chromium-v8/official/canary")
                repository.patternLayout { layout ->
                    layout.artifact("[artifact]-[revision].[ext]")
                }
                repository.metadataSources { sources -> sources.artifact() }
            }
        }
        exclusive.filter { content ->
            content.includeModule("google.d8", "v8")
        }
    }
}

fun RepositoryHandler.androidRepository() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.setUrl("https://dl.google.com/android/repository")
                repository.patternLayout { layout ->
                    layout.artifact("[artifact]-[revision].[ext]")
                    layout.artifact("[artifact]_[revision](-[classifier]).[ext]")
                    layout.artifact("[artifact]_[revision](_[classifier]).[ext]")
                }
                repository.metadataSources { sources ->
                    sources.artifact()
                }
            }
        }
        exclusive.filter { content ->
            content.includeModule("google", "platform-tools")
            content.includeModule("google", "commandlinetools-linux")
            content.includeModule("google", "commandlinetools-win")
            content.includeModule("google", "commandlinetools-mac")
            content.includeModule("google", "emulator-linux_x64")
            content.includeModule("google", "emulator-windows_x64")
            content.includeModule("google", "emulator-darwin_aarch64")
            content.includeModule("google", "android")
            content.includeModule("google", "platform")
            content.includeModule("google", "android_m2repository")
            content.includeModule("google", "build-tools")
            content.includeModuleByRegex("google", """.*\.build-tools""")
        }
    }
}

fun RepositoryHandler.androidSystemImages() {
    exclusiveContent { exclusive ->
        exclusive.forRepository {
            ivy { repository ->
                repository.setUrl("https://dl.google.com/android/repository/sys-img/android")
                repository.patternLayout { layout ->
                    layout.artifact("[artifact]-[revision](_[classifier]).[ext]")
                }
                repository.metadataSources { sources ->
                    sources.artifact()
                }
            }
        }
        exclusive.filter { content ->
            content.includeModule("google", "arm64-v8a")
            content.includeModule("google", "x86_64")
        }
    }
}
