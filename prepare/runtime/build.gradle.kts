
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
    dependsOn(configurations.getByName("build-version"))
    evaluationDependsOn(":prepare:build.version")
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
    return add(cfg.name, project(":prepare:build.version", configuration = "prepared-build-version"))
}

// TODO: move most of the code above to the root or utility script
val packRuntimeCfg = configurations.create("packed-runtime")

val outputRuntimeJarFileBase = "$buildDir/libs/kotlin-runtime"

artifacts.add("packed-runtime", File(outputRuntimeJarFileBase + ".jar"))

dependencies {
    "packed-runtime"(project(":core.builtins")) { isTransitive = false }
    "packed-runtime"(project(":libraries:stdlib")) { isTransitive = false }
    "packed-runtime"(project(":core.builtins.serialized", configuration = "default"))
    buildVersion()
}

task<ShadowJar>("pack-runtime") {
    classifier = outputRuntimeJarFileBase
    configurations = listOf(packRuntimeCfg)
    setupRuntimeJar("Kotlin Runtime")
    from(packRuntimeCfg.files)
}

//task<Jar>("aa") {
//    from(project(":core").file("descriptor.loader.java/src")) {
//        include("META-INF/services/**")
//    }
//}

defaultTasks("pack-runtime")

