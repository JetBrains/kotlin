import java.lang.reflect.Modifier
import java.net.URLClassLoader

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly("gradle.plugin.com.github.johnrengelman:shadow:${rootProject.extra["versions.shadow"]}")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

fun runPillTask(taskName: String) {
    val jarFile = configurations.archives.artifacts.single { it.type == "jar" }.file
    val cl = URLClassLoader(arrayOf(jarFile.toURI().toURL()), (object {}).javaClass.classLoader)

    val pillImporterClass = Class.forName("org.jetbrains.kotlin.pill.PillImporter", true, cl)
    val runMethod = pillImporterClass.declaredMethods.single { it.name == "run" }
    require(Modifier.isStatic(runMethod.modifiers))

    val platformDir = IntellijRootUtils.getIntellijRootDir(project)
    val resourcesDir = File(project.projectDir, "resources")
    val isIdePluginAttached = project.rootProject.intellijSdkVersionForIde() != null

    runMethod.invoke(null, project.rootProject, taskName, platformDir, resourcesDir, isIdePluginAttached)
}

val jar: Jar by tasks

val pill by tasks.creating {
    dependsOn(jar)
    doLast { runPillTask("pill") }
}

val unpill by tasks.creating {
    dependsOn(jar)
    doLast { runPillTask("unpill") }
}
