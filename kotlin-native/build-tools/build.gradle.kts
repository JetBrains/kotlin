/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlinx.metadata.jvm.KotlinModuleMetadata
import kotlinx.metadata.jvm.UnstableMetadataApi
import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.*
import java.util.jar.JarFile

buildscript {
    val rootBuildDirectory by extra(project.file("../.."))
    apply(from = rootBuildDirectory.resolve("kotlin-native/gradle/loadRootProperties.gradle"))

    dependencies {
        classpath("com.google.code.gson:gson:2.8.9")
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    groovy
    kotlin("jvm")
    `kotlin-dsl`
    id("io.github.goooler.shadow") version "8.1.7"
//    id("kotlin-build-publishing")
}

@CacheableTransformer
@OptIn(UnstableMetadataApi::class)
class KotlinModuleShadowTransformer(private val logger: Logger) : Transformer {
    @Suppress("ArrayInDataClass")
    private data class Entry(val path: String, val bytes: ByteArray)

    private val data = mutableListOf<Entry>()

    override fun getName() = "KotlinModuleShadowTransformer"

    override fun canTransformResource(element: FileTreeElement): Boolean =
            element.path.substringAfterLast(".") == KOTLIN_MODULE

    override fun transform(context: TransformerContext) {
        fun relocate(content: String): String =
                context.relocators.fold(content) { acc, relocator -> relocator.applyToSourceContent(acc) }

        logger.info("Transforming ${context.path}")
        val metadata = KotlinModuleMetadata.read(context.`is`.readBytes())
        val module = metadata.kmModule

        val packageParts = module.packageParts.toMap()
        module.packageParts.clear()
        packageParts.map { (fqName, parts) ->
            require(parts.multiFileClassParts.isEmpty()) { parts.multiFileClassParts } // There are no multi-file class parts in core

            val fileFacades = parts.fileFacades.toList()
            parts.fileFacades.clear()
            fileFacades.mapTo(parts.fileFacades) { relocate(it) }

            relocate(fqName) to parts
        }.toMap(module.packageParts)

        data += Entry(context.path, metadata.write())
    }

    override fun hasTransformedResource(): Boolean = data.isNotEmpty()

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        for ((path, bytes) in data) {
            os.putNextEntry(ZipEntry(path))
            os.write(bytes)
        }
        data.clear()
    }

    companion object {
        const val KOTLIN_MODULE = "kotlin_module"
    }
}

dependencies {
    api(gradleApi())

    api("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    // FIXME: Was this used?
    //implementation("org.jetbrains.kotlin:kotlin-reflect:${project.bootstrapKotlinVersion}") { isTransitive = false }
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")

    // FIXME: No idea why this was needed
    // To build Konan Gradle plugin
//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")

    val versionProperties = Properties()
    project.rootProject.projectDir.resolve("../../gradle/versions.properties").inputStream().use { propInput ->
        versionProperties.load(propInput)
    }
    implementation("com.google.code.gson:gson:2.8.9")
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(versionProperties["versions.gson"] as String)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    // FIXME: This wasn't used?
    // val metadataVersion = "0.0.1-dev-10"
    // implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")

    // implementation("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    // implementation("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val compileKotlin: KotlinCompile by tasks
val compileGroovy: GroovyCompile by tasks

compileKotlin.apply {
    compilerOptions {
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.addAll(
            listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
                "-Xallow-unstable-dependencies"
            )
        )
    }
}


val shadowJarTask = tasks.register<ShadowJar>("shadedNativeBuildTools") {
    destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configureEmbeddableCompilerRelocation()
}
val originalJarTask = tasks.named<Jar>("jar")

originalJarTask.configure {
//    enabled = false
    archiveClassifier.set("original")
}

val strippedMetadataJar = tasks.register<DefaultTask>("stripMetadata") {
    val outputJar = project.layout.buildDirectory.dir("stripped.jar")
    val logger = project.logger
    val inputFile = originalJarTask.map { it.archiveFile.get().asFile }

    inputs.file(inputFile)
    outputs.file(outputJar)

    dependsOn(originalJarTask)
    doLast {
        // FIXME: Strip metadata to be able to access global functions from shadowed NativePluginKt and UtilsKt
        stripMetadata(
            logger = logger,
                classesToStrip = setOf(
                    "org/jetbrains/kotlin/tools/NativePluginKt",
                    "org/jetbrains/kotlin/UtilsKt",
                ),
                inFile = inputFile.get(),
                outFile = outputJar.get().asFile
        )
    }
}

shadowJarTask.configure {
//    dependsOn(originalJarTask)
    dependsOn(strippedMetadataJar)
    from(project.layout.buildDirectory.dir("stripped.jar"))
    // FIXME: Is this wrong?
//    archiveClassifier.set("original")
    archiveClassifier.set("shadow")
}

// FIXME: This is copypasted from runtimeJar embeddable, check if it is actually needed
configurations.named("apiElements") {
    val jarFile = originalJarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}
configurations.named("runtimeElements") {
    val jarFile = originalJarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}
configurations.named("archives") {
    val jarFile = originalJarTask.get().archiveFile.get().asFile
    artifacts.removeIf { it.file == jarFile }
}

project.artifacts.add("apiElements", shadowJarTask)
project.artifacts.add("runtimeElements", shadowJarTask)
project.artifacts.add("archives", shadowJarTask)

private fun ShadowJar.configureEmbeddableCompilerRelocation() {
    relocate("org.jetbrains.kotlin", "nativebuildtools.org.jetbrains.kotlin") {
        // FIXME: These are used as mainClass = "..."
        exclude("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
        exclude("org.jetbrains.kotlin.cli.utilities.MainKt")
        exclude("org.jetbrains.kotlin.native.executors.cli.ExecutorsCLI")

        // FIXME: Not idea why this didn't work
//        project.layout.projectDirectory.asFile.resolve("preserve_list").readLines().forEach {
//            include(it)
//        }
    }
    // FIXME: This relocates parts of .kotlin_module, but does it work properly and does this affect anything?
    transform(KotlinModuleShadowTransformer(logger))
}

// Add Kotlin classes to a classpath for the Groovy compiler
compileGroovy.apply {
    classpath += project.files(compileKotlin.destinationDirectory)
    dependsOn(compileKotlin)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir(rootProject.projectDir.resolve("../../native/utils/src"))
            kotlin.srcDir(rootProject.projectDir.resolve("../../compiler/util-io/src"))
            kotlin.srcDir(rootProject.projectDir.resolve("../../compiler/util-klib/src"))
            // Copy resources for vfsoverlay hacks
            resources.srcDir(rootProject.projectDir.resolve("../../native/utils/src/main/resources"))
        }
    }
}


gradlePlugin {
    plugins {
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin"
        }
        create("runtimeTesting") {
            id = "runtime-testing"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.testing.native.RuntimeTestingPlugin"
        }
        create("compilationDatabase") {
            id = "compilation-database"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.cpp.CompilationDatabasePlugin"
        }
        create("native-interop-plugin") {
            id = "native-interop-plugin"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.NativeInteropPlugin"
        }
        create("native") {
            id = "native"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.tools.NativePlugin"
        }
        create("nativeDependenciesDownloader") {
            id = "native-dependencies-downloader"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.dependencies.NativeDependenciesDownloaderPlugin"
        }
        create("nativeDependencies") {
            id = "native-dependencies"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin"
        }
        create("platformManager") {
            id = "platform-manager"
            implementationClass = "nativebuildtools.org.jetbrains.kotlin.PlatformManagerPlugin"
        }
    }
}

private val CONSTANT_TIME_FOR_ZIP_ENTRIES = GregorianCalendar(1980, 1, 1, 0, 0, 0).timeInMillis
fun stripMetadata(logger: Logger, classesToStrip: Set<String>, inFile: File, outFile: File, preserveFileTimestamps: Boolean = true) {
    assert(inFile.exists()) { "Input file not found at $inFile" }

    fun transform(entryName: String, bytes: ByteArray): ByteArray {
        if (!entryName.endsWith(".class")) return bytes
        if (entryName.removeSuffix(".class") !in classesToStrip) return bytes

        var changed = false
        val classWriter = ClassWriter(0)
        val classVisitor = object : ClassVisitor(Opcodes.API_VERSION, classWriter) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (Type.getType(desc).internalName == "kotlin/Metadata") {
                    changed = true
                    return null
                }
                return super.visitAnnotation(desc, visible)
            }
        }
        ClassReader(bytes).accept(classVisitor, 0)
        if (!changed) return bytes

        return classWriter.toByteArray()
    }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { outJar ->
        JarFile(inFile).use { inJar ->
            for (entry in inJar.entries()) {
                val inBytes = inJar.getInputStream(entry).readBytes()
                val outBytes = transform(entry.name, inBytes)

                if (inBytes.size < outBytes.size) {
                    error("Size increased for ${entry.name}: was ${inBytes.size} bytes, became ${outBytes.size} bytes")
                }

                val newEntry = ZipEntry(entry.name)
                if (!preserveFileTimestamps) {
                    newEntry.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
                }
                outJar.putNextEntry(newEntry)
                outJar.write(outBytes)
                outJar.closeEntry()
            }
        }
    }
}
