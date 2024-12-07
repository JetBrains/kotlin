@file:Suppress("HasPlatformType")

import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import java.util.regex.Pattern.quote

description = "Kotlin Compiler"

plugins {
    // HACK: java plugin makes idea import dependencies on this project as source (with empty sources however),
    // this prevents reindexing of kotlin-compiler.jar after build on every change in compiler modules
    `java-library`
    // required to disambiguate attributes of non-jvm Kotlin libraries
    kotlin("jvm")
}


val fatJarContents by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating
val fatJarContentsStripVersions by configurations.creating

val compilerVersion by configurations.creating

// JPS build assumes fat jar is built from embedded configuration,
// but we can't use it in gradle build since slightly more complex processing is required like stripping metadata & services from some jars
if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    val embedded by configurations
    embedded.apply {
        extendsFrom(fatJarContents)
        extendsFrom(fatJarContentsStripMetadata)
        extendsFrom(fatJarContentsStripServices)
        extendsFrom(fatJarContentsStripVersions)
        extendsFrom(compilerVersion)
    }
}

val api by configurations
val proguardLibraries by configurations.creating {
    extendsFrom(api)
}

// Libraries to copy to the lib directory
val libraries by configurations.creating {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
}

val librariesStripVersion by configurations.creating

// for sbom only
val librariesKotlinTest by configurations.creating

// Compiler plugins should be copied without `kotlin-` prefix
val compilerPlugins by configurations.creating {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")

    isCanBeConsumed = false
    isCanBeResolved = true
}
val compilerPluginsCompat by configurations.creating {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")

    isCanBeConsumed = false
    isCanBeResolved = true
}

val sources by configurations.creating {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
    isTransitive = false
}

// contents of dist/maven directory
val distMavenContents by configurations.creating {
    isTransitive = false
}
// contents of dist/common directory
val distCommonContents by configurations.creating
val distStdlibMinimalForTests by configurations.creating
val buildNumber by configurations.creating

val compilerBaseName = name

val compilerModules: Array<String> by rootProject.extra

val distLibraryProjects = listOfNotNull(
    ":kotlin-annotation-processing-compiler",
    ":kotlin-annotation-processing-cli",
    ":kotlin-annotation-processing-runtime",
    ":kotlin-annotation-processing",
    ":kotlin-annotations-jvm",
    ":kotlin-ant",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    ":kotlin-imports-dumper-compiler-plugin",
    ":kotlin-main-kts",
    ":kotlin-preloader",
    // Although, Kotlin compiler is compiled against reflect of an older version (which is bundled into minimal supported IDEA). We put
    // SNAPSHOT reflect into the dist because we use reflect dist in user code compile classpath (see JvmArgumentsKt.configureStandardLibs).
    // We can use reflect of a bigger version in Kotlin compiler runtime, because kotlin-reflect follows backwards binary compatibility
    ":kotlin-reflect",
    ":kotlin-runner",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-scripting-jvm",
    ":libraries:tools:mutability-annotations-compat",
    ":plugins:android-extensions-compiler",
    ":plugins:jvm-abi-gen"
)

val distCompilerPluginProjects = listOf(
    ":kotlin-allopen-compiler-plugin",
    ":kotlin-android-extensions-runtime",
    ":plugins:parcelize:parcelize-compiler",
    ":plugins:parcelize:parcelize-runtime",
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-power-assert-compiler-plugin",
    ":kotlin-sam-with-receiver-compiler-plugin",
    ":kotlinx-serialization-compiler-plugin",
    ":kotlin-lombok-compiler-plugin",
    ":kotlin-assignment-compiler-plugin",
    ":kotlin-scripting-compiler",
    ":plugins:compose-compiler-plugin:compiler-hosted",
)
val distCompilerPluginProjectsCompat = listOf(
    ":kotlinx-serialization-compiler-plugin",
)

val distSourcesProjects = listOfNotNull(
    ":kotlin-annotations-jvm",
    ":kotlin-script-runtime",
)

configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}

dependencies {
    api(kotlinStdlib("jdk8"))
    api(project(":kotlin-script-runtime"))
    api(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    api(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
    api(libs.kotlinx.coroutines.core)

    proguardLibraries(project(":kotlin-annotations-jvm"))

    compilerVersion(project(":compiler:compiler.version"))
    proguardLibraries(project(":compiler:compiler.version"))
    compilerModules
        .filter { it != ":compiler:compiler.version" } // Version will be added directly to the final jar excluding proguard and relocation
        .forEach {
            fatJarContents(project(it)) { isTransitive = false }
        }

    libraries(kotlinStdlib("jdk8"))
    librariesKotlinTest(kotlinTest("junit"))
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        libraries(kotlinStdlib(classifier = "distJsJar"))
        libraries(kotlinStdlib(classifier = "distJsKlib"))
    }

    librariesStripVersion(libs.kotlinx.coroutines.core) { isTransitive = false }
    librariesStripVersion(commonDependency("org.jetbrains.intellij.deps:trove4j")) { isTransitive = false }

    distLibraryProjects.forEach {
        libraries(project(it)) { isTransitive = false }
    }
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        distCompilerPluginProjects.forEach {
            compilerPlugins(project(it)) { isTransitive = false }
        }
    }
    distCompilerPluginProjectsCompat.forEach {
        compilerPluginsCompat(
            project(
                mapOf(
                    "path" to it,
                    "configuration" to "distCompat"
                )
            )
        )
    }

    distSourcesProjects.forEach {
        sources(project(it, configuration = "sources"))
    }

    sources(kotlinStdlib("jdk7", classifier = "sources"))
    sources(kotlinStdlib("jdk8", classifier = "sources"))

    if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        sources(kotlinStdlib(classifier = "sources"))
        sources("org.jetbrains.kotlin:kotlin-reflect:$bootstrapKotlinVersion:sources")
    } else {
        sources(project(":kotlin-stdlib", configuration = "distSources"))
        sources(project(":kotlin-stdlib", configuration = "distJsSourcesJar"))
        sources(project(":kotlin-reflect", configuration = "sources"))

        distStdlibMinimalForTests(project(":kotlin-stdlib-jvm-minimal-for-test"))

        distCommonContents(project(":kotlin-stdlib", configuration = "commonMainMetadataElements"))
        distCommonContents(project(":kotlin-stdlib", configuration = "metadataSourcesElements"))
    }

    distMavenContents(kotlinStdlib(classifier = "sources"))

    buildNumber(project(":prepare:build.version", configuration = "buildVersion"))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        fatJarContents(kotlinBuiltins())
    }
    fatJarContents(commonDependency("javax.inject"))
    fatJarContents(commonDependency("org.jline", "jline"))
    fatJarContents(commonDependency("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDependency("com.google.code.findbugs", "jsr305"))
    fatJarContents(libs.vavr)
    fatJarContents(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm")) { isTransitive = false }

    fatJarContents(intellijCore())
    fatJarContents(commonDependency("org.jetbrains.intellij.deps.jna:jna")) { isTransitive = false }
    fatJarContents(commonDependency("org.jetbrains.intellij.deps.jna:jna-platform")) { isTransitive = false }
    fatJarContents(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    fatJarContents(commonDependency("org.lz4:lz4-java")) { isTransitive = false }
    fatJarContents(libs.intellij.asm) { isTransitive = false }
    fatJarContents(libs.guava) { isTransitive = false }
    //Gson is needed for kotlin-build-statistics. Build statistics could be enabled for JPS and Gradle builds. Gson will come from inteliij or KGP.
    proguardLibraries(commonDependency("com.google.code.gson:gson")) { isTransitive = false}

    fatJarContentsStripServices(commonDependency("com.fasterxml:aalto-xml")) { isTransitive = false }
    fatJarContents(commonDependency("org.codehaus.woodstox:stax2-api")) { isTransitive = false }

    fatJarContentsStripMetadata(commonDependency("oro:oro")) { isTransitive = false }
    fatJarContentsStripMetadata(intellijJDom()) { isTransitive = false }
    fatJarContentsStripMetadata(commonDependency("org.jetbrains.intellij.deps:log4j")) { isTransitive = false }
    fatJarContentsStripVersions(commonDependency("one.util:streamex")) { isTransitive = false }
}

val librariesKotlinTestFiles = files(
    listOf(null, "junit", "junit5", "testng", "js").map { suffix ->
        listOf(null, "sources").map { classifier ->
            configurations.detachedConfiguration(dependencies.create(kotlinTest(suffix, classifier))).apply {
                isTransitive = false
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                attributes.attribute(KlibPackaging.ATTRIBUTE, objects.named(KlibPackaging.PACKED))
            }
        }
    }
)

publish()

// sbom for dist
val distSbomTask = configureSbom(
    target = "Dist",
    documentName = "Kotlin Compiler Distribution",
    setOf(configurations.runtimeClasspath.name, libraries.name, librariesKotlinTest.name, librariesStripVersion.name, compilerPlugins.name)
)

val packCompiler by task<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("before-proguard")

    dependsOn(fatJarContents)
    from {
        fatJarContents.map(::zipTree)
    }

    dependsOn(fatJarContentsStripServices)
    from {
        fatJarContentsStripServices.files.map {
            zipTree(it).matching { exclude("META-INF/services/**") }
        }
    }

    dependsOn(fatJarContentsStripMetadata)
    from {
        fatJarContentsStripMetadata.files.map {
            zipTree(it).matching { exclude("META-INF/jb/**", "META-INF/LICENSE") }
        }
    }

    dependsOn(fatJarContentsStripVersions)
    from {
        fatJarContentsStripVersions.files.map {
            zipTree(it).matching { exclude("META-INF/versions/**") }
        }
    }
}

val proguard by task<CacheableProguardTask> {
    dependsOn(packCompiler)

    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

    configuration("$projectDir/compiler.pro")

    injars(
        mapOf("filter" to """
            !org/apache/log4j/jmx/Agent*,
            !org/apache/log4j/net/JMS*,
            !org/apache/log4j/net/SMTP*,
            !org/apache/log4j/or/jms/MessageRenderer*,
            !org/jdom/xpath/Jaxen*,
            !org/jline/builtins/ssh/**,
            !org/mozilla/javascript/xml/impl/xmlbeans/**,
            !net/sf/cglib/**,
            !META-INF/maven**,
            **.class,**.properties,**.kt,**.kotlin_*,**.jnilib,**.so,**.dll,**.txt,**.caps,
            custom-formatters.js,
            META-INF/services/**,META-INF/native/**,META-INF/extensions/**,META-INF/MANIFEST.MF,
            messages/**""".trimIndent()),
        packCompiler.map { it.outputs.files.singleFile }
    )

    outjars(layout.buildDirectory.file("libs/$compilerBaseName-after-proguard.jar"))

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraries)
    libraryjars(
        files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/jsse.jar",
                    "../Classes/jsse.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                Jvm.forHome(it.metadata.installationPath.asFile).toolsJar!!
            }
        )
    )

    printconfiguration(layout.buildDirectory.file("compiler.pro.dump"))
}

val pack: TaskProvider<out DefaultTask> = if (kotlinBuildProperties.proguard) proguard else packCompiler
val distDir: String by rootProject.extra

val jar = runtimeJar {
    dependsOn(pack)
    dependsOn(compilerVersion)

    from {
        pack.map { zipTree(it.singleOutputFile(layout)) }
    }

    from {
        compilerVersion.map(::zipTree)
    }

    manifest.attributes["Class-Path"] = compilerManifestClassPath
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
}

sourcesJar {
    from {
        compilerModules.map {
            project(it).mainSourceSet.allSource
        }
    }

    dependsOn(":compiler:fir:checkers:generateCheckersComponents", ":compiler:ir.tree:generateTree")
}

javadocJar()

val distKotlinc = distTask<Sync>("distKotlinc") {
    destinationDir = File("$distDir/kotlinc")

    from(buildNumber)

    val binFiles = files("$rootDir/compiler/cli/bin")
    into("bin") {
        from(binFiles)
    }

    val licenseFiles = files("$rootDir/license")
    into("license") {
        from(licenseFiles)
    }

    val compilerBaseName = compilerBaseName
    val jarFiles = files(jar)
    val librariesFiles = files(libraries)
    val librariesStripVersionFiles = files(librariesStripVersion)
    val sourcesFiles = files(sources)
    val compilerPluginsFiles = files(compilerPlugins)
    val compilerPluginsCompatFiles = files(compilerPluginsCompat)
    into("lib") {
        from(jarFiles) { rename { "$compilerBaseName.jar" } }
        from(librariesFiles)
        from(librariesKotlinTestFiles)
        from(librariesStripVersionFiles) {
            rename {
                it.replace(Regex("-\\d.*\\.jar\$"), ".jar")
            }
        }
        from(sourcesFiles)
        from(compilerPluginsFiles) {
            rename {
                // We want to migrate all compiler plugin in 'dist' to have 'kotlin-' prefix
                // 'kotlin-serialization-compiler-plugin' is a new jar and should have such prefix from the start
                if (!it.startsWith("kotlin-serialization")) {
                    it.removePrefix("kotlin-")
                } else {
                    it
                }
            }
        }
        from(compilerPluginsCompatFiles) {
            rename { it.removePrefix("kotlin-") }
        }
        filePermissions {
            unix("rw-r--r--")
        }
    }
}

val distCommon = distTask<Sync>("distCommon") {
    destinationDir = File("$distDir/common")
    from(distCommonContents) {
        rename { name ->
            name
                .replace("-metadata.jar", "-common.jar")
                .replace("-metadata.klib", "-common.klib")
                .replace("-metadata-sources.jar", "-common-sources.jar")
        }
    }
}

val distMaven = distTask<Sync>("distMaven") {
    destinationDir = File("$distDir/maven")
    from(distMavenContents)
}

distTask<Copy>("dist") {
    destinationDir = File(distDir)

    dependsOn(distKotlinc)
    dependsOn(distCommon)
    dependsOn(distMaven)
    dependsOn(distSbomTask)

    from(buildNumber)
    from(distStdlibMinimalForTests)
    from(distSbomTask) {
        rename(".*", "${project.name}-${project.version}.spdx.json")
    }
}

inline fun <reified T : AbstractCopyTask> Project.distTask(
    name: String,
    crossinline block: T.() -> Unit
) = tasks.register<T>(name) {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-$version"), "")
    rename(quote("-$bootstrapKotlinVersion"), "")
    block()
}
