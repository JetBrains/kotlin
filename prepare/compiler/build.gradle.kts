@file:Suppress("HasPlatformType")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask
import java.util.regex.Pattern.quote

description = "Kotlin Compiler"

plugins {
    // HACK: java plugin makes idea import dependencies on this project as source (with empty sources however),
    // this prevents reindexing of kotlin-compiler.jar after build on every change in compiler modules
    java
}

// You can run Gradle with "-Pkotlin.build.proguard=true" to enable ProGuard run on kotlin-compiler.jar (on TeamCity, ProGuard always runs)
val shrink = findProperty("kotlin.build.proguard")?.toString()?.toBoolean() ?: hasProperty("teamcity")

val jsIrDist = findProperty("kotlin.stdlib.js.ir.dist")?.toString()?.toBoolean() == true

val fatJarContents by configurations.creating
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating

val runtimeJar by configurations.creating
val compile by configurations  // maven plugin writes pom compile scope from compile configuration by default
val proguardLibraries by configurations.creating {
    extendsFrom(compile)
}

// Libraries to copy to the lib directory
val libraries by configurations.creating
// Compiler plugins should be copied without `kotlin-` prefix
val compilerPlugins by configurations.creating
val sources by configurations.creating
// contents of dist/maven directory
val distMavenContents by configurations.creating
// contents of dist/common directory
val distCommonContents by configurations.creating
val distStdlibMinimalForTests by configurations.creating
val buildNumber by configurations.creating
val distJSContents by configurations.creating

val compilerBaseName = name

val outputJar = fileFrom(buildDir, "libs", "$compilerBaseName.jar")

val compilerModules: Array<String> by rootProject.extra

val distLibraryProjects = listOfNotNull(
    ":kotlin-annotation-processing",
    ":kotlin-annotation-processing-cli",
    ":kotlin-annotation-processing-runtime",
    ":kotlin-annotations-android",
    ":kotlin-annotations-jvm",
    ":kotlin-ant",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    ":kotlin-daemon-client-new",
    ":kotlin-imports-dumper-compiler-plugin",
    ":kotlin-main-kts",
    ":kotlin-preloader",
    ":kotlin-reflect",
    ":kotlin-runner",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-scripting-jvm",
    ":kotlin-stdlib-js-ir".takeIf { jsIrDist },
    ":kotlin-source-sections-compiler-plugin",
    ":kotlin-test:kotlin-test-js",
    ":kotlin-test:kotlin-test-js-ir".takeIf { jsIrDist },
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-testng",
    ":libraries:tools:mutability-annotations-compat",
    ":plugins:android-extensions-compiler",
    ":plugins:jvm-abi-gen"
)

val distCompilerPluginProjects = listOf(
    ":kotlin-allopen-compiler-plugin",
    ":kotlin-android-extensions-runtime",
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-sam-with-receiver-compiler-plugin",
    ":kotlinx-serialization-compiler-plugin"
)

val distSourcesProjects = listOfNotNull(
    ":kotlin-annotations-jvm",
    ":kotlin-reflect",
    ":kotlin-script-runtime",
    ":kotlin-stdlib-jdk7".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
    ":kotlin-stdlib-jdk8".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
    ":kotlin-stdlib-js-ir".takeIf { jsIrDist },
    ":kotlin-test:kotlin-test-js",
    ":kotlin-test:kotlin-test-js-ir".takeIf { jsIrDist },
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-testng"
)

libraries.apply {
    resolutionStrategy {
        preferProjectModules()
    }

    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    proguardLibraries(project(":kotlin-annotations-jvm"))
    proguardLibraries(
        files(
            firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
            firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
            toolsJar()
        )
    )

    compilerModules.forEach {
        fatJarContents(project(it)) { isTransitive = false }
    }

    libraries(intellijDep()) { includeIntellijCoreJarDependencies(project) { it.startsWith("trove4j") } }
    libraries(commonDep("io.ktor", "ktor-network"))
    libraries(kotlinStdlib("jdk8"))
    libraries(kotlinStdlib("js"))

    distLibraryProjects.forEach {
        libraries(project(it)) { isTransitive = false }
    }

    distCompilerPluginProjects.forEach {
        compilerPlugins(project(it)) { isTransitive = false }
    }

    distSourcesProjects.forEach {
        sources(project(it, configuration = "sources"))
    }

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        sources(project(":kotlin-stdlib", configuration = "distSources"))
        sources(project(":kotlin-stdlib-js", configuration = "distSources"))

        distStdlibMinimalForTests(project(":kotlin-stdlib:jvm-minimal-for-test"))

        distJSContents(project(":kotlin-stdlib-js", configuration = "distJs"))
        distJSContents(project(":kotlin-test:kotlin-test-js", configuration = "distJs"))
    }

    distCommonContents(kotlinStdlib(suffix = "common"))
    distCommonContents(kotlinStdlib(suffix = "common", classifier = "sources"))

    distMavenContents(kotlinStdlib(classifier = "sources"))

    buildNumber(project(":prepare:build.version", configuration = "buildVersion"))

    fatJarContents(kotlinBuiltins())
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))
    fatJarContents(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }

    if (Platform.P192.orHigher()) {
        fatJarContents(intellijDep()) { includeJars("lz4-java-1.6.0") }
    } else {
        fatJarContents(intellijDep()) { includeJars("lz4-1.3.0") }
    }
    
    if (Platform.P183.orHigher() && Platform.P191.orLower()) {
        fatJarContents(intellijCoreDep()) { includeJars("java-compatibility-1.0.1") }
    }

    fatJarContents(intellijDep()) {
        includeIntellijCoreJarDependencies(project) {
            !(it.startsWith("jdom") || it.startsWith("log4j") || it.startsWith("trove4j"))
        }
    }

    fatJarContentsStripServices(jpsStandalone()) { includeJars("jps-model") }
    
    fatJarContentsStripMetadata(intellijDep()) { includeJars("oro-2.0.8", "jdom", "log4j" ) }
}

publish()

noDefaultJar()

val packCompiler by task<ShadowJar> {
    configurations = emptyList()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(File(buildDir, "libs"))

    setupPublicJar(compilerBaseName, "before-proguard")
    
    from(fatJarContents)

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

    manifest.attributes["Class-Path"] = compilerManifestClassPath
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
}

val proguard by task<ProGuardTask> {
    dependsOn(packCompiler)
    configuration("$rootDir/compiler/compiler.pro")

    val outputJar = fileFrom(buildDir, "libs", "$compilerBaseName-after-proguard.jar")

    inputs.files(packCompiler.outputs.files.singleFile)
    outputs.file(outputJar)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraries)

    printconfiguration("$buildDir/compiler.pro.dump")

    // This properties are used by proguard config compiler.pro
    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", packCompiler.outputs.files.singleFile.canonicalPath)
        System.setProperty("kotlin-compiler-jar", outputJar.canonicalPath)
    }
}

val pack = if (shrink) proguard else packCompiler

val distDir: String by rootProject.extra

val distKotlinc = distTask<Sync>("distKotlinc") {
    destinationDir = File("$distDir/kotlinc")

    from(buildNumber)

    into("bin") {
        from(files("$rootDir/compiler/cli/bin"))
    }

    into("license") {
        from(files("$rootDir/license"))
    }

    into("lib") {
        from(pack) { rename { "$compilerBaseName.jar" } }
        from(libraries)
        from(sources)
        from(compilerPlugins) {
            rename { it.removePrefix("kotlin-") }
        }
    }
}

val distCommon = distTask<Sync>("distCommon") {
    destinationDir = File("$distDir/common")
    from(distCommonContents)
}

val distMaven = distTask<Sync>("distMaven") {
    destinationDir = File("$distDir/maven")
    from(distMavenContents)
}

val distJs = distTask<Sync>("distJs") {
    destinationDir = File("$distDir/js")
    from(distJSContents)
}

distTask<Copy>("dist") {
    destinationDir = File(distDir)

    dependsOn(distKotlinc)
    dependsOn(distCommon)
    dependsOn(distMaven)
    dependsOn(distJs)

    from(buildNumber)
    from(distStdlibMinimalForTests)
}

runtimeJarArtifactBy(pack, pack.outputs.files.singleFile) {
    name = compilerBaseName
    classifier = ""
}

sourcesJar {
    from {
        compilerModules.map {
            project(it).mainSourceSet.allSource
        }
    }
}

javadocJar()

inline fun <reified T : AbstractCopyTask> Project.distTask(
    name: String,
    crossinline block: T.() -> Unit
) = tasks.register<T>(name) {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-$version"), "")
    block()
}