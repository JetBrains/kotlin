import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import org.gradle.kotlin.dsl.apiBuild
import org.gradle.kotlin.dsl.invoke
import java.util.jar.JarFile
import kotlin.sequences.forEach

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

publish()

standardPublicJars()

dependencies {
    embedded(project(":libraries:tools:abi-validation:abi-tools")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
}

runtimeJarWithRelocation {
    relocate("org.jetbrains.kotlin", "org.jetbrains.kotlin.abi.tools.org.jetbrains.kotlin") {
        // avoid recursive relocation
        exclude("org.jetbrains.kotlin.abi.tools.**")
    }
    relocate("javax", "org.jetbrains.kotlin.abi.tools.javax")
    relocate("kotlin.script", "org.jetbrains.kotlin.abi.tools.kotlin.script")
    relocate("kotlin.metadata", "org.jetbrains.kotlin.abi.tools.kotlin.metadata")
    relocate("kotlin.annotations", "org.jetbrains.kotlin.abi.tools.kotlin.annotations")
    relocate("org.jetbrains.annotations", "org.jetbrains.kotlin.abi.tools.org.jetbrains.annotations")
    relocate("com.github.difflib", "org.jetbrains.kotlin.abi.tools.com.github.difflib")
    relocate("org.checkerframework", "org.jetbrains.kotlin.abi.tools.org.checkerframework")

    exclude("kotlinManifest.properties", "META-INF/compiler.version", "**/module-info.class")

    // also relocate SPI files and its content
    mergeServiceFiles()
}

tasks.register<CheckPackagesTask>("checkPackages") {
    val shadowJar = tasks.shadowJar
    dependsOn(shadowJar)

    packageName = "org.jetbrains.kotlin.abi.tools"
    jar.set(shadowJar.flatMap { it.archiveFile })
}


/**
 * Task to check that all classes in [jar] are placed in the specified [packageName] package.
 */
abstract class CheckPackagesTask : DefaultTask() {
    @get:InputFile
    abstract val jar: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @TaskAction
    fun check() {
        val dir = packageName.get().removeSuffix(".").replace('.', '/') + "/"
        JarFile(jar.get().asFile).use { jarFile ->
            jarFile.entries().asSequence().forEach { entry ->
                // skip not class-files
                if (entry.isDirectory || !entry.name.endsWith(".class")) return@forEach

                if (!entry.name.startsWith(dir)) {
                    throw AssertionError("Incorrect shadowing of classes, all classes should be placed in '${packageName.get()}' package but class-file ${entry.name} was missed.\nPlease fix relocation rules")
                }
            }
        }
    }
}


tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar") {
    // remove all original kotlin leftovers
    exclude("kotlin/**")
}

apiValidation {
    ignoredPackages.add("org.jetbrains.kotlin.abi.tools.com")
    ignoredPackages.add("org.jetbrains.kotlin.abi.tools.kotlin")
    ignoredPackages.add("org.jetbrains.kotlin.abi.tools.org")
    ignoredPackages.add("org.jetbrains.kotlin.abi.tools.javax")
}

tasks.apiBuild {
    dependsOn(tasks.shadowJar)
    inputJar.value(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.check {
    dependsOn("checkPackages")
}


