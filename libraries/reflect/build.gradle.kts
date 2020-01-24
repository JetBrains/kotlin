import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import kotlinx.metadata.jvm.KmModuleVisitor
import kotlinx.metadata.jvm.KotlinModuleMetadata
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import proguard.gradle.ProGuardTask
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

description = "Kotlin Full Reflection Library"

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0")
    }
}

plugins {
    java
}

callGroovy("configureJavaOnlyJvm6Project", project)

publish()

val core = "$rootDir/core"
val relocatedCoreSrc = "$buildDir/core-relocated"
val libsDir = property("libsDir")

val proguardDeps by configurations.creating
val proguardAdditionalInJars by configurations.creating

val embedded by configurations
embedded.isTransitive = false

configurations.getByName("compileOnly").extendsFrom(embedded)
val mainJar by configurations.creating

dependencies {
    compile(kotlinStdlib())

    proguardDeps(kotlinStdlib())
    proguardAdditionalInJars(project(":kotlin-annotations-jvm"))
    proguardDeps(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar", jdkHome = File(property("JDK_16") as String))))

    embedded(project(":core:type-system"))
    embedded(project(":kotlin-reflect-api"))
    embedded(project(":core:metadata"))
    embedded(project(":core:metadata.jvm"))
    embedded(project(":core:descriptors"))
    embedded(project(":core:descriptors.jvm"))
    embedded(project(":core:deserialization"))
    embedded(project(":core:descriptors.runtime"))
    embedded(project(":core:util.runtime"))
    embedded("javax.inject:javax.inject:1")
    embedded(protobufLite())
    
    compileOnly("org.jetbrains:annotations:13.0")
}

@CacheableTransformer
class KotlinModuleShadowTransformer(private val logger: Logger) : Transformer {
    @Suppress("ArrayInDataClass")
    private data class Entry(val path: String, val bytes: ByteArray)
    private val data = mutableListOf<Entry>()

    override fun canTransformResource(element: FileTreeElement): Boolean =
            element.path.substringAfterLast(".") == KOTLIN_MODULE

    override fun transform(context: TransformerContext) {
        fun relocate(content: String): String =
                context.relocators.fold(content) { acc, relocator -> relocator.applyToSourceContent(acc) }

        val metadata = KotlinModuleMetadata.read(context.`is`.readBytes())
                ?: error("Not a .kotlin_module file: ${context.path}")
        val writer = KotlinModuleMetadata.Writer()
        logger.info("Transforming ${context.path}")
        metadata.accept(object : KmModuleVisitor(writer) {
            override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
                assert(multiFileClassParts.isEmpty()) { multiFileClassParts } // There are no multi-file class parts in core
                super.visitPackageParts(relocate(fqName), fileFacades.map(::relocate), multiFileClassParts)
            }
        })
        data += Entry(context.path, writer.write().bytes)
    }

    override fun hasTransformedResource(): Boolean =
            data.isNotEmpty()

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

val reflectShadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    configurations = listOf(embedded)

    callGroovy("manifestAttributes", manifest, project, "Main" /*true*/)

    exclude("**/*.proto")

    if (kotlinBuildProperties.relocation) {
        mergeServiceFiles()
        transform(KotlinModuleShadowTransformer(logger))
        relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
        relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    }
}

val stripMetadata by tasks.registering {
    dependsOn(reflectShadowJar)
    val inputJar = provider { reflectShadowJar.get().outputs.files.singleFile }
    val outputJar = File("$libsDir/kotlin-reflect-stripped.jar")
    inputs.file(inputJar).withPathSensitivity(RELATIVE)
    outputs.file(outputJar)
    outputs.cacheIf { true }
    doLast {
        stripMetadata(
            logger = logger,
            classNamePattern = "kotlin/reflect/jvm/internal/impl/.*",
            inFile = inputJar.get(),
            outFile = outputJar,
            preserveFileTimestamps = false
        )
    }
}

val proguardOutput = "$libsDir/${property("archivesBaseName")}-proguard.jar"

val proguard by task<ProGuardTask> {
    dependsOn(stripMetadata)
    inputs.files(stripMetadata.get().outputs.files)
    outputs.file(proguardOutput)

    injars(mapOf("filter" to "!META-INF/versions/**"), stripMetadata.get().outputs.files)
    injars(mapOf("filter" to "!META-INF/**,!**/*.kotlin_builtins"), proguardAdditionalInJars)
    outjars(proguardOutput)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardDeps)

    configuration("$core/reflection.jvm/reflection.pro")
}

val relocateCoreSources by task<Copy> {
    doFirst {
        delete(relocatedCoreSrc)
    }

    from("$core/descriptors/src")
    from("$core/descriptors.jvm/src")
    from("$core/descriptors.runtime/src")
    from("$core/deserialization/src")
    from("$core/util.runtime/src")

    exclude("META-INF/services/**")

    into(relocatedCoreSrc)
    includeEmptyDirs = false

    eachFile {
        path = path.replace("org/jetbrains/kotlin", "kotlin/reflect/jvm/internal/impl")
    }

    filter { line ->
        line.replace("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
    }
}

tasks.getByName("jar").enabled = false

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")

    dependsOn(relocateCoreSources)
    from(relocatedCoreSrc)
    from("$core/reflection.jvm/src")
}

addArtifact("archives", sourcesJar)
addArtifact("sources", sourcesJar)

val intermediate = when {
    kotlinBuildProperties.proguard -> proguard
    kotlinBuildProperties.relocation -> stripMetadata
    else -> reflectShadowJar
}
    
val result by task<Jar> {
    dependsOn(intermediate)
    from {
        zipTree(intermediate.get().outputs.files.singleFile)
    }
    callGroovy("manifestAttributes", manifest, project, "Main")
}

val modularJar by task<Jar> {
    dependsOn(intermediate)
    archiveClassifier.set("modular")
    from(zipTree(intermediate.get().outputs.files.single()))
    from(zipTree(reflectShadowJar.get().archivePath)) {
        include("META-INF/versions/**")
    }
    callGroovy("manifestAttributes", manifest, project, "Main", true)
}

val dexMethodCount by task<DexMethodCount> {
    dependsOn(result)
    jarFile = result.get().outputs.files.single()
    ownPackages = listOf("kotlin.reflect")
}
tasks.getByName("check").dependsOn(dexMethodCount)

artifacts {
    listOf(mainJar.name, "runtime", "archives").forEach { configurationName ->
        add(configurationName, result.get().outputs.files.singleFile) {
            builtBy(result)
        }
    }

    add("archives", modularJar)
}

javadocJar()
