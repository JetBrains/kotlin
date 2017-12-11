import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeTask

buildscript {
    repositories {
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // for intellij plugin
        maven(url = "http://dl.bintray.com/jetbrains/intellij-plugin-service") // for intellij plugin
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.3.0-SNAPSHOT")
    }
}

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    runtimeOnly(files(toolsJar()))
    compileOnly(intellijUltimateDep())
}

afterEvaluate {
    val ideaUltimatePluginDir: File by rootProject.extra
    val ideaUltimateSandboxDir: File by rootProject.extra

    tasks.findByName("runIde")?.let { tasks.remove(it) }
    tasks.findByName("prepareSandbox")?.let { tasks.remove(it) }

    val prepareSandbox by task<PrepareSandboxTask> {
        configDirectory = File(ideaUltimateSandboxDir, "config")
        // the rest are just some "fake" values, to keeping PrepareSandboxTask happy
        setPluginName("Kotlin")
        dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin", ":ultimate:idea-ultimate-plugin")
        setPluginJar(File(ideaUltimatePluginDir, "lib/kotlin-script-runtime.jar")) // any small jar will do
        destinationDir = File(buildDir, "sandbox-fake")
    }

    task<RunIdeTask>("runUltimate") {
        dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin", ":ultimate:idea-ultimate-plugin")
        dependsOn(prepareSandbox)
        group = "intellij"
        description = "Runs Intellij IDEA Ultimate with installed plugin."
        setIdeaDirectory(intellijRootDir())
        setConfigDirectory(File(ideaUltimateSandboxDir, "config"))
        setSystemDirectory(ideaUltimateSandboxDir)
        setPluginsDirectory(ideaUltimatePluginDir.parent)
        jvmArgs(
                "-Xmx1250m",
                "-XX:ReservedCodeCacheSize=240m",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-ea",
                "-Didea.is.internal=true",
                "-Didea.debug.mode=true",
                "-Dapple.laf.useScreenMenuBar=true",
                "-Dapple.awt.graphics.UseQuartz=true",
                "-Dsun.io.useCanonCaches=false",
                "-Dkotlin.internal.mode.enabled=true"
        )
        if (project.hasProperty("noPCE")) {
            jvmArgs("-Didea.ProcessCanceledException=disabled")
        }
    }
}
