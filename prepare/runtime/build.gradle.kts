
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(files("$rootDir/ideaSDK/lib/asm-all.jar"))
    }
}

repositories {
    mavenCentral()
}

apply { plugin("com.github.johnrengelman.shadow") }

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", rootProject.extra["build.number"])
    }
    from(configurations.getByName("build-version").files) {
        into("META-INF/")
    }
}

fun DependencyHandler.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return add(cfg.name, project(":prepare:build.version", configuration = "default"))
}

fun DependencyHandler.projectDep(name: String): Dependency = project(name, configuration = "default")
fun DependencyHandler.projectDepIntransitive(name: String): Dependency =
        project(name, configuration = "default").apply { isTransitive = false }

// TODO: common ^ 8< ----

val mainCfg = configurations.create("default")

val outputRuntimeJarFileBase = "$buildDir/libs/kotlin-runtime"

artifacts.add(mainCfg.name, File(outputRuntimeJarFileBase + ".jar"))

dependencies {
    mainCfg.name(projectDepIntransitive(":core.builtins"))
    mainCfg.name(projectDepIntransitive(":libraries:stdlib"))
    mainCfg.name(project(":core.builtins.serialized", configuration = "default"))
    buildVersion()
}

val mainTask = task<ShadowJar>("prepare") {
    classifier = outputRuntimeJarFileBase
    configurations = listOf(mainCfg)
    dependsOn(":core.builtins.serialized:prepare", ":core.builtins:assemble", ":libraries:stdlib:assemble")
    setupRuntimeJar("Kotlin Runtime")
    from(mainCfg.files)
}

defaultTasks(mainTask.name)

