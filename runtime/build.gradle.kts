
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
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
    }
}

apply { plugin("com.github.johnrengelman.shadow") }

repositories {
    mavenCentral()
}

val packCfg = configurations.create("packed-runtime")

val outputJarPath = "$buildDir/libs/kotlin-runtime.jar"

artifacts.add("packed-runtime", File(outputJarPath))

dependencies {
    "packed-runtime"(project(":core.builtins", configuration = "default"))
    "packed-runtime"(project(":libraries:stdlib")) {
        isTransitive = false
    }
    "packed-runtime"(project(":core.builtins.serialized")) {
        isTransitive = false
    }
}

val packTask = task<ShadowJar>("pack-runtime") {
    classifier = "$buildDir/libs/kotlin-runtime"
    this.configurations = listOf(packCfg)
    from(packCfg.files)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
}

defaultTasks("pack-runtime")

