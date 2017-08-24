
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin IDEA plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

val projectsToShadow = listOf(
        ":core:builtins",
        ":plugins:annotation-based-compiler-plugins-ide-support",
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core",
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
        ":idea:idea-jps-common",
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
    packedJars(commonDep("com.github.spullara.cli-parser", "cli-parser"))
    packedJars(preloadedDeps("protobuf-${rootProject.extra["versions.protobuf-java"]}"))
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(preloadedDeps("markdown", "kotlinx-coroutines-core", "kotlinx-coroutines-jdk8"))
}

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    projectsToShadow.forEach {
        dependsOn("$it:classes")
        project(it).let { p ->
            p.pluginManager.withPlugin("java") {
                from(p.the<JavaPluginConvention>().sourceSets.getByName("main").output)
            }
        }
    }
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(packedJars.files)
}

ideaPlugin {
    from(jar)
    from(sideJars)
}

