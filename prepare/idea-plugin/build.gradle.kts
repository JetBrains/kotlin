import java.util.regex.Pattern.quote

description = "Kotlin IDEA plugin"

plugins {
    `java-base`
}

repositories {
    maven("https://jetbrains.bintray.com/markdown")
}

// PILL: used in pill importer
val projectsToShadow by extra(listOf(
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":core:type-system",
        ":compiler:backend",
        ":compiler:backend-common",
        ":compiler:backend.jvm",
        ":compiler:ir.backend.common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core:metadata",
        ":core:metadata.jvm",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization",
        ":idea:eval4j",
        ":idea:formatter",
        ":compiler:psi",
        *if (project.findProperty("fir.enabled") == "true") {
            arrayOf(
                ":compiler:fir:cones",
                ":compiler:fir:resolve",
                ":compiler:fir:tree",
                ":compiler:fir:java",
                ":compiler:fir:psi2fir",
                ":compiler:fir:fir2ir",
                ":idea:fir-view"
            )
        } else {
            emptyArray()
        },
        ":compiler:frontend",
        ":compiler:frontend.common",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-native",
        ":idea:idea-core",
        ":idea:idea-gradle",
        ":idea:idea-gradle-native",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.serializer",
        ":js:js.translator",
        ":kotlin-native:kotlin-native-utils",
        ":kotlin-native:kotlin-native-library-reader",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":core:util.runtime",
        ":plugins:lint",
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea",
        ":j2k",
        ":nj2k",
        ":nj2k:nj2k-services",
        ":kotlin-allopen-compiler-plugin",
        ":kotlin-noarg-compiler-plugin",
        ":kotlin-sam-with-receiver-compiler-plugin",
        ":kotlin-scripting-idea",
        ":kotlinx-serialization-compiler-plugin",
        ":kotlinx-serialization-ide-plugin",
        ":idea:idea-android",
        ":idea:idea-android-output-parser",
        ":idea:idea-jvm",
        ":idea:idea-git",
        ":idea:idea-jps-common",
        ":plugins:android-extensions-compiler",
        *if (Ide.IJ())
            arrayOf(":idea:idea-maven")
        else
            emptyArray<String>()
))

// Gradle tooling model jars are loaded into Gradle during import and should present in plugin as separate jar
val gradleToolingModel by configurations.creating

val libraries by configurations.creating {
    extendsFrom(gradleToolingModel)
}

val jpsPlugin by configurations.creating

configurations.all {
    resolutionStrategy {
        preferProjectModules()
    }

    exclude("org.jetbrains.intellij.deps", "trove4j") // Idea already has trove4j
}

dependencies {
    projectsToShadow.forEach {
        embedded(project(it)) { isTransitive = false }
    }
    embedded(protobufFull())
    embedded(kotlinBuiltins())

    libraries(commonDep("javax.inject"))
    libraries(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8"))
    libraries(commonDep("org.jetbrains", "markdown"))
    libraries(commonDep("io.javaslang", "javaslang"))

    libraries(kotlinStdlib("jdk8"))
    libraries(project(":kotlin-reflect"))
    libraries(project(":kotlin-compiler-client-embeddable"))
    libraries(project(":kotlin-daemon-client"))
    libraries(project(":kotlin-script-runtime"))
    libraries(project(":kotlin-script-util"))
    libraries(project(":kotlin-scripting-common"))
    libraries(project(":kotlin-scripting-impl"))
    libraries(project(":kotlin-scripting-intellij"))
    libraries(project(":kotlin-scripting-jvm"))

    gradleToolingModel(project(":idea:kotlin-gradle-tooling")) { isTransitive = false }
    gradleToolingModel(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    gradleToolingModel(project(":plugins:kapt3-idea")) { isTransitive = false }
    gradleToolingModel(project(":plugins:android-extensions-ide")) { isTransitive = false }
    gradleToolingModel(project(":noarg-ide-plugin")) { isTransitive = false }
    gradleToolingModel(project(":allopen-ide-plugin")) { isTransitive = false }

    jpsPlugin(project(":kotlin-jps-plugin"))
}

val jar = runtimeJar {
    from("$rootDir/resources/kotlinManifest.properties")
    archiveName = "kotlin-plugin.jar"
}

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
}
