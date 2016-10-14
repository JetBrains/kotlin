
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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

val packRuntimeCfg = configurations.create("packed-runtime")

val outputRuntimeJarFileBase = "$buildDir/libs/kotlin-runtime"

artifacts.add("packed-runtime", File(outputRuntimeJarFileBase + ".jar"))

dependencies {
    "packed-runtime"(project(":core.builtins")) { isTransitive = false }
    "packed-runtime"(project(":libraries:stdlib")) { isTransitive = false }
    "packed-runtime"(project(":core.builtins.serialized", configuration = "default"))
    "packed-runtime"(project(":prepare:build.version", configuration = "default"))
}

task<ShadowJar>("pack-runtime") {
    classifier = outputRuntimeJarFileBase
    configurations = listOf(packRuntimeCfg)
    from(packRuntimeCfg.files)
}

//task<Jar>("aa") {
//    from(project(":core").file("descriptor.loader.java/src")) {
//        include("META-INF/services/**")
//    }
//}

defaultTasks("pack-runtime")

