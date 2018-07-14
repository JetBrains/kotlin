import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

description = "Kotlin IDEA plugin"

plugins {
    `java-base`
    id("com.github.jk1.tcdeps") version "0.17"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val kotlinNativeVersion = "0.9-dev-2798"

// Do not rename, used in JPS importer
val projectsToShadow by extra(listOf(
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core:metadata",
        ":core:metadata.jvm",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization",
        ":eval4j",
        ":idea:formatter",
        ":compiler:psi",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-native",
        ":idea:idea-core",
        ":idea:idea-gradle",
        ":idea:idea-gradle:native",
        //":idea-ultimate",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
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
        ":core:util.runtime"))

// Do not rename, used in JPS importer
val packedJars by configurations.creating

val sideJars by configurations.creating

dependencies {
    packedJars(protobufFull())
    packedJars(project(":core:builtins", configuration = "builtins"))
    sideJars(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:shared.jar"))
    sideJars(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:backend.native.jar"))
    sideJars("org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.9-dev-2809") { isTransitive = false }
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(projectDist(":kotlin-stdlib"))
    sideJars(projectDist(":kotlin-reflect"))
    sideJars(project(":kotlin-compiler-client-embeddable"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    sideJars(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    sideJars(commonDep("org.jetbrains", "markdown")) { isTransitive = false }
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
    duplicatesStrategy = DuplicatesStrategy.FAIL // Investigation is required if we have multiple jars with same name
    dependsOn(":dist")
    from(jar)
    from(sideJars)
}

