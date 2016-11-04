
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

val mainCfg = configurations.create("default")

val outputRuntimeJarFileBase = "$buildDir/libs/kotlin-runtime"

artifacts.add(mainCfg.name, File(outputRuntimeJarFileBase + ".jar"))

dependencies {
    mainCfg.name(projectDepIntransitive(":core.builtins"))
    mainCfg.name(projectDepIntransitive(":libraries:stdlib"))
    buildVersion()
}

val mainTask = task<ShadowJar>("prepare") {
    classifier = outputRuntimeJarFileBase
    configurations = listOf(mainCfg)
    dependsOn(":core.builtins:assemble", ":libraries:stdlib:assemble")
    setupRuntimeJar("Kotlin Runtime")
    from(mainCfg.files)
}

defaultTasks(mainTask.name)

