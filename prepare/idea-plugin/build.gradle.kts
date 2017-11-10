
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin IDEA plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins {
    `java-base`
}

val projectsToShadow = listOf(
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization",
        ":eval4j",
        ":idea:formatter",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-android",
        ":idea:idea-android-output-parser",
        ":idea:idea-core",
        ":idea:idea-jvm",
        ":idea:idea-jps-common",
        ":idea:idea-gradle",
        ":idea:idea-maven",
        //":idea-ultimate",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":j2k",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.serializer",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":core:util.runtime")

val packedJars by configurations.creating
val sideJars by configurations.creating

dependencies {
    packedJars(protobufFull())
    packedJars(project(":core:builtins", configuration = "builtins"))
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(projectDist(":kotlin-stdlib"))
    sideJars(projectDist(":kotlin-reflect"))
    sideJars(project(":kotlin-compiler-client-embeddable"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    sideJars(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    sideJars("teamcity:markdown")
}

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(packedJars)
    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output)
    }
    archiveName = "kotlin-plugin.jar"
}

ideaPlugin {
    shouldRunAfter(":dist")
    from(jar)
    from(sideJars)
}

