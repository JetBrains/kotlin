
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

val embedCfg = configurations.create("embed")
val mainCfg = configurations.create("default")

val embeddableCompilerBaseName: String by rootProject.extra

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
        listOf("com.intellij",
               "com.google",
               "com.sampullara",
               "org.apache",
               "org.jdom",
               "org.picocontainer",
               "jline",
               "gnu",
               "javax.inject",
               "org.fusesource")

dependencies {
    embedCfg(project(":prepare:compiler", configuration = "default"))
}

val embeddableTask = task<ShadowJar>("prepare") {
    destinationDir = File(buildDir, "libs")
    baseName = embeddableCompilerBaseName
    configurations = listOf(mainCfg)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(":build-common:assemble", ":core:script.runtime:assemble")
    from(embedCfg.files)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

defaultTasks(embeddableTask.name)

artifacts.add(mainCfg.name, embeddableTask.outputs.files.singleFile) {
    builtBy(embeddableTask)
    classifier = ""
}
