import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.*
import java.net.URL

plugins {
    base
    id("org.jetbrains.dokka")
}

val isTeamcityBuild = project.hasProperty("teamcity.version")

// kotlin/libraries/tools/kotlin-stdlib-docs  ->  kotlin
val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs by extra("$buildDir/libs")

val rootProperties = java.util.Properties().apply {
    file(kotlin_root).resolve("gradle.properties").inputStream().use { stream -> load(stream) }
}
val defaultSnapshotVersion: String by rootProperties
val kotlinLanguageVersion: String by rootProperties

val githubRevision = if (isTeamcityBuild) project.property("githubRevision") else "master"
val artifactsVersion by extra(if (isTeamcityBuild) project.property("deployVersion") as String else defaultSnapshotVersion)
val artifactsRepo by extra(if (isTeamcityBuild) project.property("kotlinLibsRepo") as String else "$kotlin_root/build/repo")
val dokka_version: String by project

println("# Parameters summary:")
println("    isTeamcityBuild: $isTeamcityBuild")
println("    dokka version: $dokka_version")
println("    githubRevision: $githubRevision")
println("    language version: $kotlinLanguageVersion")
println("    artifacts version: $artifactsVersion")
println("    artifacts repo: $artifactsRepo")


val outputDir = file(findProperty("docsBuildDir") as String? ?: "$buildDir/doc")
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
val templatesDir = file(findProperty("templatesDir") as String? ?: "$projectDir/templates").invariantSeparatorsPath

val cleanDocs by tasks.registering(Delete::class) {
    delete(outputDir)
}

tasks.clean {
    dependsOn(cleanDocs)
}

val prepare by tasks.registering {
    dependsOn(":kotlin_big:extractLibs")
}

dependencies {
    dokkaPlugin(project(":plugins:dokka-samples-transformer-plugin"))
    dokkaPlugin(project(":plugins:dokka-version-filter-plugin"))
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:$dokka_version")
}

fun createStdLibVersionedDocTask(version: String, isLatest: Boolean) =
    tasks.register<DokkaTaskPartial>("kotlin-stdlib_" + version + (if (isLatest) "_latest" else "")) {
        dependsOn(prepare)

        val kotlin_stdlib_dir = file("$kotlin_root/libraries/stdlib")

        val stdlibIncludeMd = file("$kotlin_root/libraries/stdlib/src/Module.md")
        val stdlibSamples = file("$kotlin_root/libraries/stdlib/samples/test")

        val suppressedPackages = listOf(
                "kotlin.internal",
                "kotlin.jvm.internal",
                "kotlin.js.internal",
                "kotlin.native.internal",
                "kotlin.jvm.functions",
                "kotlin.coroutines.jvm.internal",
        )

        val kotlinLanguageVersion = version

        moduleName.set("kotlin-stdlib")
        val moduleDirName = "kotlin-stdlib"
        with(pluginsMapConfiguration) {
            put("org.jetbrains.dokka.base.DokkaBase"                      , """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""")
            put("org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin", """{ "ignoreCommonBuiltIns": "true" }""")
            put("org.jetbrains.dokka.versioning.VersioningPlugin"         , """{ "version": "$version" }" }""")
        }
        if (isLatest) {
            outputDirectory.set(file("$outputDirPartial/latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(file("$outputDirPartial/previous").resolve(moduleDirName).resolve(version))
            pluginsMapConfiguration
                .put("org.jetbrains.dokka.kotlinlang.VersionFilterPlugin"      , """{ "targetVersion": "$version" }""")
        }
        dokkaSourceSets {
            register("common") {
                jdkVersion.set(8)
                platform.set(Platform.common)
                noJdkLink.set(true)

                displayName.set("Common")
                sourceRoots.from("$kotlin_root/core/builtins/native")
                sourceRoots.from("$kotlin_root/core/builtins/src/")

                sourceRoots.from("$kotlin_stdlib_dir/common/src")
                sourceRoots.from("$kotlin_stdlib_dir/src")
                sourceRoots.from("$kotlin_stdlib_dir/unsigned/src")
            }

            register("jvm") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)

                displayName.set("JVM")
                dependsOn("common")

                sourceRoots.from("$kotlin_stdlib_dir/jvm/src")

                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/annotations")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/JvmClassMapping.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/PurelyImplements.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Metadata.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Throws.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/TypeAliases.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/text/TypeAliases.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jdk7/src")
                sourceRoots.from("$kotlin_stdlib_dir/jdk8/src")
            }
            register("js") {
                jdkVersion.set(8)
                platform.set(Platform.js)
                noJdkLink.set(true)

                displayName.set("JS")
                dependsOn("common")

                // list src subdirectories except 'generated' as it should be taken from js-ir/src
                sourceRoots.from("$kotlin_stdlib_dir/js/src/kotlin")
                sourceRoots.from("$kotlin_stdlib_dir/js/src/kotlinx")
                sourceRoots.from("$kotlin_stdlib_dir/js/src/org.w3c")

                sourceRoots.from("$kotlin_stdlib_dir/js-ir/builtins")
                sourceRoots.from("$kotlin_stdlib_dir/js-ir/runtime/kotlinHacks.kt")
                sourceRoots.from("$kotlin_stdlib_dir/js-ir/runtime/long.kt")
                sourceRoots.from("$kotlin_stdlib_dir/js-ir/src")

                perPackageOption("org.w3c") {
                    reportUndocumented.set(false)
                }
                perPackageOption("org.khronos") {
                    reportUndocumented.set(false)
                }
            }
            register("native") {
                jdkVersion.set(8)
                platform.set(Platform.native)
                noJdkLink.set(true)

                displayName.set("Native")
                dependsOn("common")

                sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/main/kotlin")
                sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/native/kotlin")
                sourceRoots.from("$kotlin_native_root/Interop/JsRuntime/src/main/kotlin")
                sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin")
                sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
                perPackageOption("kotlin.test") {
                    suppress.set(true)
                }
            }
            configureEach {
                documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED))
                skipDeprecated.set(false)
                includes.from(stdlibIncludeMd)
                noStdlibLink.set(true)
                languageVersion.set(kotlinLanguageVersion)
                samples.from(stdlibSamples.toString())
                suppressedPackages.forEach { packageName ->
                    perPackageOption(packageName) {
                        suppress.set(true)
                    }
                }
                sourceLinksFromRoot()
            }
        }
    }

fun createKotlinReflectVersionedDocTask(version: String, isLatest: Boolean) =
    tasks.register<DokkaTaskPartial>("kotlin-reflect_" + version + (if (isLatest) "_latest" else "")) {
        dependsOn(prepare)

        val kotlinReflectIncludeMd = file("$kotlin_root/libraries/reflect/Module.md")

        val kotlinReflectClasspath = fileTree("$kotlin_libs/kotlin-reflect")

        val kotlinLanguageVersion = version

        moduleName.set("kotlin-reflect")

        val moduleDirName = "kotlin-reflect"
        with(pluginsMapConfiguration) {
            put("org.jetbrains.dokka.base.DokkaBase", """{ "templatesDir": "$templatesDir" }""")
            put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version" }""")
        }
        if (isLatest) {
            outputDirectory.set(file("$outputDirPartial/latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(file("$outputDirPartial/previous").resolve(moduleDirName).resolve(version))
            pluginsMapConfiguration.put("org.jetbrains.dokka.kotlinlang.VersionFilterPlugin", """{ "targetVersion": "$version" }""")
        }

        dokkaSourceSets {
            register("jvm") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinReflectClasspath)

                displayName.set("JVM")
                sourceRoots.from("$kotlin_root/core/reflection.jvm/src")

                skipDeprecated.set(false)
                includes.from(kotlinReflectIncludeMd)
                languageVersion.set(kotlinLanguageVersion)
                noStdlibLink.set(true)
                perPackageOption("kotlin.reflect.jvm.internal") {
                    suppress.set(true)
                }
                sourceLinksFromRoot()
            }
        }
    }

fun createKotlinTestVersionedDocTask(version: String, isLatest: Boolean) =
    tasks.register<DokkaTaskPartial>("kotlin-test_" + version + (if (isLatest) "_latest" else "")) {
        dependsOn(prepare)

        val kotlinTestIncludeMd = file("$kotlin_root/libraries/kotlin.test/Module.md")

        val kotlinTestCommonClasspath = fileTree("$kotlin_libs/kotlin-test-common")
        val kotlinTestJunitClasspath = fileTree("$kotlin_libs/kotlin-test-junit")
        val kotlinTestJunit5Classpath = fileTree("$kotlin_libs/kotlin-test-junit5")
        val kotlinTestTestngClasspath = fileTree("$kotlin_libs/kotlin-test-testng")
        val kotlinTestJsClasspath = fileTree("$kotlin_libs/kotlin-test-js")
        val kotlinTestJvmClasspath = fileTree("$kotlin_libs/kotlin-test")

        val kotlinLanguageVersion = version

        moduleName.set("kotlin-test")

        val moduleDirName = "kotlin-test"
        with(pluginsMapConfiguration) {
            put("org.jetbrains.dokka.base.DokkaBase", """{ "templatesDir": "$templatesDir" }""")
            put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version" }""")
        }
        if (isLatest) {
            outputDirectory.set(file("$outputDirPartial/latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(file("$outputDirPartial/previous").resolve(moduleDirName).resolve(version))
            pluginsMapConfiguration.put("org.jetbrains.dokka.kotlinlang.VersionFilterPlugin", """{ "targetVersion": "$version" }""")
        }

        dokkaSourceSets {
            register("common") {
                jdkVersion.set(8)
                platform.set(Platform.common)
                classpath.setFrom(kotlinTestCommonClasspath)
                noJdkLink.set(true)

                displayName.set("Common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/common/src/main/kotlin")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/annotations-common/src/main/kotlin")
            }

            register("jvm") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJvmClasspath)

                displayName.set("JVM")
                dependsOn("common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/jvm/src/main/kotlin")
            }

            register("jvm-JUnit") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJunitClasspath)

                displayName.set("JUnit")
                dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit/src/main/kotlin")

                externalDocumentationLink {
                    url.set(URL("http://junit.org/junit4/javadoc/latest/"))
                    packageListUrl.set(URL("http://junit.org/junit4/javadoc/latest/package-list"))
                }
            }

            register("jvm-JUnit5") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJunit5Classpath)

                displayName.set("JUnit5")
                dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit5/src/main/kotlin")

                externalDocumentationLink {
                    url.set(URL("https://junit.org/junit5/docs/current/api/"))
                    packageListUrl.set(URL("https://junit.org/junit5/docs/current/api/element-list"))
                }
            }

            register("jvm-TestNG") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestTestngClasspath)

                displayName.set("TestNG")
                dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/testng/src/main/kotlin")

                // externalDocumentationLink {
                //     url.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/"))
                //     packageListUrl.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/package-list"))
                // }
            }
            register("js") {
                platform.set(Platform.js)
                classpath.setFrom(kotlinTestJsClasspath)
                noJdkLink.set(true)

                displayName.set("JS")
                dependsOn("common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/js/src/main/kotlin")
            }
            register("native") {
                platform.set(Platform.native)
                noJdkLink.set(true)

                displayName.set("Native")
                dependsOn("common")
                sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin/kotlin/test")
            }
            configureEach {
                skipDeprecated.set(false)
                includes.from(kotlinTestIncludeMd)
                languageVersion.set(kotlinLanguageVersion)
                noStdlibLink.set(true)
                sourceLinksFromRoot()
            }
        }
    }


fun createAllLibsVersionedDocTask(version: String, isLatest: Boolean, vararg libTasks: TaskProvider<DokkaTaskPartial>) =
    tasks.register<DokkaMultiModuleTask>("all-libs_" + version + (if (isLatest) "_latest" else "")) {
        moduleName.set("Kotlin libraries")
        plugins.extendsFrom(configurations.dokkaHtmlMultiModulePlugin.get())
        runtime.extendsFrom(configurations.dokkaHtmlMultiModuleRuntime.get())
        libTasks.forEach { addChildTask(it.name) }

        fileLayout.set(DokkaMultiModuleFileLayout { parent, child ->
            parent.outputDirectory.dir(child.moduleName)
        })

        val moduleDirName = "all-libs"
        val outputDirLatest = file("$outputDir/latest")
        val outputDirPrevious = file("$outputDir/previous")
        pluginsMapConfiguration.put("org.jetbrains.dokka.base.DokkaBase", """{ "templatesDir": "$templatesDir" }""")
        if (isLatest) {
            outputDirectory.set(outputDirLatest.resolve(moduleDirName))
            pluginsMapConfiguration.put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version", "olderVersionsDir": "${inputDirPrevious.resolve(moduleDirName).invariantSeparatorsPath}" }""")
        } else {
            outputDirectory.set(outputDirPrevious.resolve(moduleDirName).resolve(version))
            pluginsMapConfiguration.put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version" }""")
        }
    }

fun GradleDokkaSourceSetBuilder.perPackageOption(packageNamePrefix: String, action: Action<in GradlePackageOptionsBuilder>) =
    perPackageOption {
        matchingRegex.set(Regex.escape(packageNamePrefix) + "(\$|\\..*)")
        action(this)
    }

fun GradleDokkaSourceSetBuilder.sourceLinksFromRoot() {
    sourceLink {
        localDirectory.set(file(kotlin_root))
        remoteUrl.set(URL("https://github.com/JetBrains/kotlin/tree/$githubRevision"))
        remoteLineSuffix.set("#L")
    }
}

run {
    val versions = listOf(/*"1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7",*/ kotlinLanguageVersion)
    val latestVersion = versions.last()

    // builds this version/all versions as historical for the next versions builds
    val buildAllVersions by tasks.registering
    // builds the latest version incorporating all previous historical versions docs
    val buildLatestVersion by tasks.registering

    val latestStdlib = createStdLibVersionedDocTask(latestVersion, true)
    val latestReflect = createKotlinReflectVersionedDocTask(latestVersion, true)
    val latestTest = createKotlinTestVersionedDocTask(latestVersion, true)
    val latestAll = createAllLibsVersionedDocTask(latestVersion, true, latestStdlib, latestReflect, latestTest)

    buildLatestVersion.configure { dependsOn(latestStdlib, latestTest, latestReflect, latestAll) }

    versions.forEach { version ->
        val versionStdlib = createStdLibVersionedDocTask(version, false)
        val versionReflect = createKotlinReflectVersionedDocTask(version, false)
        val versionTest = createKotlinTestVersionedDocTask(version, false)
        val versionAll = createAllLibsVersionedDocTask(version, isLatest = false, versionStdlib, versionReflect, versionTest)
        if (version != latestVersion) {
            latestAll.configure { dependsOn(versionAll) }
        }
        buildAllVersions.configure { dependsOn(versionStdlib, versionTest, versionAll) }
    }
}
