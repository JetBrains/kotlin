import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.*
import java.net.URL

plugins {
    base
    id("org.jetbrains.dokka")
}

val isTeamcityBuild = project.hasProperty("teamcity.version") || System.getenv("TEAMCITY_VERSION") != null

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
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")
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
                "kotlin.wasm.internal",
        )

        val kotlinLanguageVersion = version

        moduleName.set("kotlin-stdlib")
        val moduleDirName = "kotlin-stdlib"
        with(pluginsMapConfiguration) {
            put("org.jetbrains.dokka.base.DokkaBase"                      , """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""")
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

                sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins")

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

                sourceRoots.from("$kotlin_stdlib_dir/js/src/generated")
                sourceRoots.from("$kotlin_stdlib_dir/js/src/kotlin")

                sourceRoots.from("$kotlin_stdlib_dir/js/builtins")

                // builtin sources that are copied from common builtins during JS stdlib build
                listOf(
                    "Annotation.kt",
                    "Any.kt",
                    "CharSequence.kt",
                    "Comparable.kt",
                    "Iterator.kt",
                    "Nothing.kt",
                    "Number.kt",
                ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }

                perPackageOption("kotlin.browser") {
                    suppress.set(true)
                }
                perPackageOption("kotlin.dom") {
                    suppress.set(true)
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
                sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin")
                sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
                perPackageOption("kotlin.test") {
                    suppress.set(true)
                }
            }
            register("wasm-js") {
                platform.set(Platform.wasm)
                noJdkLink.set(true)

                displayName.set("Wasm-JS")
                dependsOn("common")
                sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/src")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/builtins")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/internal")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/stubs")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/js/builtins")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/js/internal")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/js/src")

                // builtin sources that are copied from common builtins during Wasm stdlib build
                listOf(
                    "Annotation.kt",
                    "CharSequence.kt",
                    "Comparable.kt",
                    "Number.kt",
                ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }
            }
            register("wasm-wasi") {
                platform.set(Platform.wasm)
                noJdkLink.set(true)

                displayName.set("Wasm-WASI")
                dependsOn("common")
                sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/src")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/builtins")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/internal")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/stubs")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/wasi/builtins")
                sourceRoots.from("$kotlin_stdlib_dir/wasm/wasi/src")

                // builtin sources that are copied from common builtins during Wasm stdlib build
                listOf(
                    "Annotation.kt",
                    "CharSequence.kt",
                    "Comparable.kt",
                    "Number.kt",
                ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }
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
        fixIntersectedSourceRootsAndSamples(dokkaSourceSets, "stdlib")
    }

fun createKotlinReflectVersionedDocTask(version: String, isLatest: Boolean) =
    tasks.register<DokkaTaskPartial>("kotlin-reflect_" + version + (if (isLatest) "_latest" else "")) {
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")
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
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")
        dependsOn(prepare)

        val kotlinTestIncludeMd = file("$kotlin_root/libraries/kotlin.test/Module.md")

        val kotlinTestCommonClasspath = fileTree("$kotlin_libs/kotlin-stdlib-common")
        val kotlinTestJunitClasspath = fileTree("$kotlin_libs/kotlin-test-junit")
        val kotlinTestJunit5Classpath = fileTree("$kotlin_libs/kotlin-test-junit5")
        val kotlinTestTestngClasspath = fileTree("$kotlin_libs/kotlin-test-testng")
        val kotlinTestJsClasspath = fileTree("$kotlin_libs/kotlin-test-js")
        val kotlinTestJvmClasspath = fileTree("$kotlin_libs/kotlin-test")

        val kotlinLanguageVersion = version

        moduleName.set("kotlin-test")

        val moduleDirName = "kotlin-test"
        with(pluginsMapConfiguration) {
            put("org.jetbrains.dokka.base.DokkaBase", """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""")
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
            register("wasm-js") {
                platform.set(Platform.wasm)
                noJdkLink.set(true)

                displayName.set("Wasm-JS")
                dependsOn("common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/src/main")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/js/src/main")
            }
            register("wasm-wasi") {
                platform.set(Platform.wasm)
                noJdkLink.set(true)

                displayName.set("Wasm-WASI")
                dependsOn("common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/src/main")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/wasi/src/main")
            }
            configureEach {
                skipDeprecated.set(false)
                includes.from(kotlinTestIncludeMd)
                languageVersion.set(kotlinLanguageVersion)
                noStdlibLink.set(true)
                sourceLinksFromRoot()
            }
        }
        fixIntersectedSourceRootsAndSamples(dokkaSourceSets, "kotlin.test")
    }


fun createAllLibsVersionedDocTask(version: String, isLatest: Boolean, vararg libTasks: TaskProvider<DokkaTaskPartial>) =
    tasks.register<DokkaMultiModuleTask>("all-libs_" + version + (if (isLatest) "_latest" else "")) {
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")
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
            pluginsMapConfiguration.put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version", "olderVersionsDirName": "", "olderVersionsDir": "${inputDirPrevious.resolve(moduleDirName).invariantSeparatorsPath}" }""")
        } else {
            outputDirectory.set(outputDirPrevious.resolve(moduleDirName).resolve(version))
            pluginsMapConfiguration.put("org.jetbrains.dokka.versioning.VersioningPlugin", """{ "version": "$version" }""")
        }

        doLast {
            // copy package-list files from partial tasks of single modules
            libTasks.map { it.get() }.forEach { child ->
                val originalOutput = child.outputDirectory
                val mergedOutput = outputDirectory.dir(child.moduleName)
                project.copy {
                    from(originalOutput.file("package-list"))
                    into(mergedOutput)
                }
            }
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

/**
 * The Dokka K2 does not support intersecting source or sample roots
 * https://github.com/Kotlin/dokka/issues/3701
 *
 * As a workaround, the intersecting roots may be copied and source links should be fixed
 *
 * This function detects such source and sample roots, copy them and fix source links.
 * It should be called after all other configurations
 */
fun AbstractDokkaTask.fixIntersectedSourceRootsAndSamples(
    dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder>,
    libraryName: String
) {
    val kotlin_library_dir = file("$kotlin_root/libraries/$libraryName")

    fun intersectOfNormalizedPaths(normalizedPaths: Set<File>, normalizedPaths2: Set<File>): Set<File> {
        val result = mutableSetOf<File>()
        for (p1 in normalizedPaths) {
            for (p2 in normalizedPaths2) {
                if (p1.startsWith(p2) || p2.startsWith(p1)) {
                    result.add(p1)
                    result.add(p2)
                }
            }
        }
        return result
    }

    fun Set<File>.normalize() = mapTo(mutableSetOf()) { it.normalize() }
    fun intersect(paths: Set<File>, paths2: Set<File>): Set<File> = intersectOfNormalizedPaths(paths.normalize(), paths2.normalize())

    val sourceSets = dokkaSourceSets.toList()
    val temporaryDirectory = buildDir.resolve("temporary_dokka_source_sets/$libraryName/").toPath()

    val replacementsSources = mutableMapOf<String, MutableMap<File, File>>()
    val replacementsSamples = mutableMapOf<String, MutableMap<File, File>>()

    for (i in sourceSets.indices) {
        for (j in i + 1 until sourceSets.size) {
            intersect(
                sourceSets[i].sourceRoots.toSet(),
                sourceSets[j].sourceRoots.toSet()
            ).forEach {
                val relativePath = kotlin_library_dir.toPath().relativize(it.toPath())
                replacementsSources.getOrPut(sourceSets[i].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[i].name).resolve(relativePath).toFile()
                replacementsSources.getOrPut(sourceSets[j].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[j].name).resolve(relativePath).toFile()
            }

            intersect(
                sourceSets[i].samples.toSet(),
                sourceSets[j].samples.toSet()
            ).forEach {
                val relativePath = kotlin_library_dir.toPath().relativize(it.toPath())
                replacementsSamples.getOrPut(sourceSets[i].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[i].name).resolve(relativePath).toFile()
                replacementsSamples.getOrPut(sourceSets[j].name, ::mutableMapOf)[it] =
                    temporaryDirectory.resolve(sourceSets[j].name).resolve(relativePath).toFile()
            }
        }
    }
    replacementsSamples.forEach { (sourceSetName, replacements) ->
        val sourceSet = dokkaSourceSets[sourceSetName]

        // replace samples here
        sourceSet.samples.setFrom(sourceSet.samples.map { replacements[it] ?: it })
    }

    val kotlin_library_url = "https://github.com/JetBrains/kotlin/tree/$githubRevision/libraries/$libraryName"
    replacementsSources.forEach { (sourceSetName, replacements) ->
        val sourceSet = dokkaSourceSets[sourceSetName]
        // replace sourceRoots here
        sourceSet.sourceRoots.setFrom(sourceSet.sourceRoots.map { replacements[it] ?: it })

        replacements.forEach { (original, replacement) ->
            // setup source-links
            sourceSet.sourceLink {
                remoteUrl.set(URL("$kotlin_library_url/${kotlin_library_dir.toPath().relativize(original.toPath())}"))
                localDirectory.set(replacement)
                remoteLineSuffix.set("#L")
            }
        }

        // The order of source links is important
        // source links to temporary directories should have higher priority
        sourceSet.sourceLinks.set(sourceSet.sourceLinks.get().reversed())

        // work with files
        doFirst {
            temporaryDirectory.toFile().deleteRecursively()
            replacementsSamples.forEach { (_, replacements) ->
                replacements.forEach { (original, replacement) ->
                    // copy files
                    original.copyRecursively(replacement, overwrite = true)
                }
            }
            replacementsSources.forEach { (_, replacements) ->
                replacements.forEach { (original, replacement) ->
                    // copy files
                    original.copyRecursively(replacement, overwrite = true)
                }
            }
        }
    }
}