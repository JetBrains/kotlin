import org.jetbrains.kotlin.pill.PillExtension
import java.lang.reflect.Modifier
import java.net.URLClassLoader

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly("com.github.jengelman.gradle.plugins:shadow:${rootProject.extra["versions.shadow"]}")
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
    runMethod.invoke(null, project.rootProject, taskName, platformDir, resourcesDir, EmbeddedComponents.CONFIGURATION_NAME)
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