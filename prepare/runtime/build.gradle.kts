
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(ideaSdkDeps("asm-all"))
    }
}

apply { plugin("com.github.johnrengelman.shadow") }

val mainCfg = configurations.create("default")

val outputRuntimeJarFileBase = "$buildDir/libs/kotlin-runtime"

artifacts.add(mainCfg.name, File(outputRuntimeJarFileBase + ".jar"))

dependencies {
    mainCfg.name(projectDepIntransitive(":core:builtins"))
    mainCfg.name(projectDepIntransitive(":kotlin-stdlib"))
    buildVersion()
}

val mainTask = task<ShadowJar>("prepare") {
    classifier = outputRuntimeJarFileBase
    configurations = listOf(mainCfg)
    dependsOn(":core:builtins:assemble", ":kotlin-stdlib:assemble")
    setupRuntimeJar("Kotlin Runtime")
    from(mainCfg.files)
}

defaultTasks(mainTask.name)

