import java.util.regex.Pattern.quote

description = "Kotlin IDEA plugin"

plugins {
    java
}

repositories {
    maven("https://jetbrains.bintray.com/markdown")
}

// PILL: used in pill importer
val projectsToShadow by extra(listOf(
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":compiler:backend",
        ":compiler:resolution.common.jvm",
        ":core:compiler.common.jvm",
        ":compiler:backend.common.jvm",
        ":compiler:backend-common",
        ":compiler:backend.jvm",
        ":compiler:ir.backend.common",
        ":compiler:ir.serialization.jvm",
        ":compiler:ir.serialization.common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":daemon-common",
        ":daemon-common-new",
        ":core:metadata",
        ":core:metadata.jvm",
        ":core:compiler.common",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization.common",
        ":core:deserialization.common.jvm",
        ":core:deserialization",
        ":idea:jvm-debugger:eval4j",
        ":idea:jvm-debugger:jvm-debugger-util",
        ":idea:jvm-debugger:jvm-debugger-core",
        ":idea:jvm-debugger:jvm-debugger-evaluation",
        ":idea:jvm-debugger:jvm-debugger-coroutine",
        ":idea:jvm-debugger:jvm-debugger-sequence",
        ":idea:scripting-support",
        ":idea:idea-j2k",
        ":idea:formatter",
        ":idea:line-indent-provider",
        ":compiler:psi",
        ":compiler:fir:cones",
        ":compiler:fir:checkers",
        ":compiler:fir:entrypoint",
        ":compiler:fir:resolve",
        ":compiler:fir:fir-serialization",
        ":compiler:fir:fir-deserialization",
        ":compiler:fir:tree",
        ":compiler:fir:java",
        ":compiler:fir:jvm",
        ":compiler:fir:raw-fir:psi2fir",
        ":compiler:fir:raw-fir:raw-fir.common",
        ":compiler:fir:fir2ir",
        ":compiler:fir:fir2ir:jvm-backend",
        ":compiler:frontend",
        ":compiler:frontend.common",
        ":compiler:frontend.java",
        ":compiler:frontend:cfg",
        ":idea",
        ":idea:idea-native",
        ":idea:idea-core",
        ":idea:idea-gradle",
        ":idea:idea-gradle-native",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":compiler:ir.tree.impl",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.config",
        ":js:js.serializer",
        ":js:js.translator",
        ":native:kotlin-native-utils",
        ":native:frontend.native",
        ":kotlin-gradle-statistics",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution.common",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":compiler:config",
        ":compiler:config.jvm",
        ":compiler:compiler.version",
        ":core:util.runtime",
        ":plugins:lint",
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea",
        ":j2k",
        ":nj2k",
        ":nj2k:nj2k-services",
        ":kotlin-scripting-idea",
        ":kotlinx-serialization-ide-plugin",
        ":idea:idea-android",
        ":idea:idea-android-output-parser",
        ":idea:idea-jvm",
        ":idea:idea-git",
        ":idea:idea-jps-common",
        ":idea:idea-frontend-independent",
        ":idea:idea-frontend-fir",
        ":idea:idea-frontend-api",
        ":idea:idea-frontend-fir:idea-fir-low-level-api",
        ":idea:idea-fir-performance-tests",
        ":idea:idea-fir",
        *if (Ide.IJ())
            arrayOf(
                ":idea:idea-maven",
                ":libraries:tools:new-project-wizard",
                ":idea:idea-new-project-wizard",
                ":libraries:tools:new-project-wizard:new-project-wizard-cli"
            )
        else
            emptyArray<String>()
    )
)

// Projects published to maven copied to the plugin as separate jars
val libraryProjects = listOf(
    ":kotlin-reflect",
    ":kotlin-compiler-client-embeddable",
    ":kotlin-daemon-client",
    ":kotlin-daemon-client-new",
    ":kotlin-daemon",
    ":kotlin-script-runtime",
    ":kotlin-script-util",
    ":kotlin-scripting-common",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-scripting-intellij",
    ":kotlin-scripting-jvm",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":kotlin-allopen-compiler-plugin",
    ":kotlin-noarg-compiler-plugin",
    ":kotlin-sam-with-receiver-compiler-plugin",
    ":plugins:android-extensions-compiler",
    ":plugins:parcelize:parcelize-compiler",
    ":kotlinx-serialization-compiler-plugin",
    ":idea:ide-common"
)

// Gradle tooling model jars are loaded into Gradle during import and should present in plugin as separate jar
val gradleToolingModel by configurations.creating

val libraries by configurations.creating {
    extendsFrom(gradleToolingModel)
    exclude("org.jetbrains.intellij.deps", "trove4j") // Idea already has trove4j
}

val jpsPlugin by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }
}

dependencies {
    projectsToShadow.forEach {
        embedded(project(it)) { isTransitive = false }
    }
    embedded(protobufFull())
    embedded(kotlinBuiltins(forJvm = true))

    libraries(commonDep(kotlinxCollectionsImmutable()))
    libraries(commonDep("javax.inject"))
    libraries(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8"))
    libraries(commonDep("org.jetbrains", "markdown"))
    libraries(commonDep("io.javaslang", "javaslang"))

    libraries(kotlinStdlib("jdk8"))

    libraries(commonDep("org.jetbrains.intellij.deps.completion", "completion-ranking-kotlin"))

    libraryProjects.forEach {
        libraries(project(it)) { isTransitive = false }
    }

    gradleToolingModel(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    gradleToolingModel(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    gradleToolingModel(project(":plugins:kapt3-idea")) { isTransitive = false }
    gradleToolingModel(project(":plugins:android-extensions-ide")) { isTransitive = false }
    gradleToolingModel(project(":plugins:parcelize:parcelize-ide")) { isTransitive = false }
    gradleToolingModel(project(":noarg-ide-plugin")) { isTransitive = false }
    gradleToolingModel(project(":allopen-ide-plugin")) { isTransitive = false }

    jpsPlugin(project(":kotlin-jps-plugin")) { isTransitive = false }

    (libraries.dependencies + gradleToolingModel.dependencies)
        .map { if (it is ProjectDependency) it.dependencyProject else it }
        .forEach(::compile)
}

val jar = runtimeJar {
    from("$rootDir/resources/kotlinManifest.properties")
    archiveFileName.set("kotlin-plugin.jar")
}.get() // make it eager to avoid corresponding refactorings in the kotlin-ultimate part for now

sourcesJar()

javadocJar()

val ideaPluginDir: File by rootProject.extra
tasks.register<Sync>("ideaPlugin") {
    dependsOn(":dist")

    into(File(ideaPluginDir, "lib"))

    duplicatesStrategy = DuplicatesStrategy.FAIL // Investigation is required if we have multiple jars with same name

    from(jar)
    from(libraries)
    from(jpsPlugin) {
        into("jps")
    }

    rename(quote("-$version"), "")
    rename(quote("-$bootstrapKotlinVersion"), "")
}

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
