@file:Suppress("HasPlatformType")

import java.util.regex.Pattern.quote

description = "Kotlin Compiler"

plugins {
    // HACK: java plugin makes idea import dependencies on this project as source (with empty sources however),
    // this prevents reindexing of kotlin-compiler.jar after build on every change in compiler modules
    java
}

val JDK_18: String by rootProject.extra

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

val compile by configurations  // maven plugin writes pom compile scope from compile configuration by default
val proguardLibraries by configurations.creating {
    extendsFrom(compile)
}

// Libraries to copy to the lib directory
val libraries by configurations.creating {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
}

val librariesStripVersion by configurations.creating

// Compiler plugins should be copied without `kotlin-` prefix
val compilerPlugins by configurations.creating  {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
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
val distJSContents by configurations.creating

val compilerBaseName = name

val outputJar = fileFrom(buildDir, "libs", "$compilerBaseName.jar")

val compilerModules: Array<String> by rootProject.extra

val distLibraryProjects = listOfNotNull(
    ":kotlin-annotation-processing",
    ":kotlin-annotation-processing-cli",
    ":kotlin-annotation-processing-runtime",
    ":kotlin-annotations-jvm",
    ":kotlin-ant",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    // TODO: uncomment when new daemon will be put back into dist
//    ":kotlin-daemon-client-new",
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
    ":kotlin-scripting-js",
    ":js:js.engines",
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
    ":plugins:parcelize:parcelize-compiler",
    ":plugins:parcelize:parcelize-runtime",
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-sam-with-receiver-compiler-plugin",
    ":kotlinx-serialization-compiler-plugin"
)

val distSourcesProjects = listOfNotNull(
    ":kotlin-annotations-jvm",
    ":kotlin-script-runtime",
    ":kotlin-test:kotlin-test-js".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-testng"
)

configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    proguardLibraries(project(":kotlin-annotations-jvm"))

    compilerVersion(project(":compiler:compiler.version"))
    proguardLibraries(project(":compiler:compiler.version"))
    compilerModules
        .filter { it != ":compiler:compiler.version" } // Version will be added directly to the final jar excluding proguard and relocation
        .forEach {
            fatJarContents(project(it)) { isTransitive = false }
        }

    libraries(intellijDep()) { includeIntellijCoreJarDependencies(project) { it.startsWith("trove4j") } }
    libraries(kotlinStdlib("jdk8"))
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        libraries(kotlinStdlib("js", "distLibrary"))
        libraries(project(":kotlin-test:kotlin-test-js", configuration = "distLibrary"))
    }

    librariesStripVersion(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    distLibraryProjects.forEach {
        libraries(project(it)) { isTransitive = false }
    }

    distCompilerPluginProjects.forEach {
        compilerPlugins(project(it)) { isTransitive = false }
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
        sources(project(":kotlin-stdlib-js", configuration = "distSources"))
        sources(project(":kotlin-reflect", configuration = "sources"))

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
    fatJarContents(commonDep("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm")) { isTransitive = false }

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }

    if (Platform.P202()) {
        fatJarContents(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-1") }
    } else if (Platform.P203.orHigher()) {
        fatJarContents(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-3") }
    }

    fatJarContents(intellijDep()) { includeJars("lz4-java", rootProject = rootProject) }

    fatJarContents(intellijDep()) {
        includeIntellijCoreJarDependencies(project) {
            !(it.startsWith("jdom") || it.startsWith("log4j") || it.startsWith("trove4j") || it.startsWith("streamex"))
        }
    }

    fatJarContentsStripServices(jpsStandalone()) { includeJars("jps-model") }

    fatJarContentsStripMetadata(intellijDep()) { includeJars("oro-2.0.8", "jdom", "log4j" ) }

    fatJarContentsStripVersions(intellijCoreDep()) { includeJars("streamex", rootProject = rootProject) }
}

publish()

val packCompiler by task<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(File(buildDir, "libs"))
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

    jdkHome = File(JDK_18)

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
            META-INF/services/**,META-INF/native/**,META-INF/extensions/**,META-INF/MANIFEST.MF,
            messages/**""".trimIndent()),
        provider { packCompiler.get().outputs.files.singleFile }
    )

    outjars(fileFrom(buildDir, "libs", "$compilerBaseName-after-proguard.jar"))

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraries)
    libraryjars(
        files(
            firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar", jdkHome = jdkHome!!),
            firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar", jdkHome = jdkHome!!),
            toolsJarFile(jdkHome = jdkHome!!)
        )
    )

    printconfiguration("$buildDir/compiler.pro.dump")
}

val pack = if (kotlinBuildProperties.proguard) proguard else packCompiler
val distDir: String by rootProject.extra

val jar = runtimeJar {
    dependsOn(pack)
    dependsOn(compilerVersion)

    from {
        zipTree(pack.get().singleOutputFile())
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
    into("lib") {
        from(jarFiles) { rename { "$compilerBaseName.jar" } }
        from(librariesFiles)
        from(librariesStripVersionFiles) {
            rename {
                it.replace(Regex("-\\d.*\\.jar\$"), ".jar")
            }
        }
        from(sourcesFiles)
        from(compilerPluginsFiles) {
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

inline fun <reified T : AbstractCopyTask> Project.distTask(
    name: String,
    crossinline block: T.() -> Unit
) = tasks.register<T>(name) {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-$version"), "")
    rename(quote("-$bootstrapKotlinVersion"), "")
    block()
}
