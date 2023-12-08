import java.lang.reflect.Modifier
import java.net.URLClassLoader
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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
    compileOnly(libs.shadow.gradlePlugin)
    compileOnly(libs.jdom2)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

fun runPillTask(taskName: String) {
    val jarFile = configurations.archives.get().artifacts.single { it.type == "jar" }.file
    val cl = URLClassLoader(arrayOf(jarFile.toURI().toURL()), (object {}).javaClass.classLoader)

    val pillImporterClass = Class.forName("org.jetbrains.kotlin.pill.PillImporter", true, cl)
    val runMethod = pillImporterClass.declaredMethods.single { it.name == "run" }
    require(Modifier.isStatic(runMethod.modifiers))

    val platformDir = rootProject.ideaHomePathForTests().get().asFile
    val resourcesDir = File(project.projectDir, "resources")

    runMethod.invoke(null, project.rootProject, taskName, platformDir, resourcesDir)
}

val jar: Jar by tasks

val pill by tasks.creating {
    dependsOn(jar)
    dependsOn(":createIdeaHomeForTests")
    doLast { runPillTask("pill") }
}

val unpill by tasks.creating {
    dependsOn(jar)
    doLast { runPillTask("unpill") }
}
