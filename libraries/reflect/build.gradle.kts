import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import kotlinx.metadata.jvm.KmModuleVisitor
import kotlinx.metadata.jvm.KotlinModuleMetadata
import proguard.gradle.ProGuardTask
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

description = "Kotlin Full Reflection Library"

buildscript {
    repositories {
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.0.4")
    }
}

plugins {
    java
    id("pill-configurable")
}

callGroovy("configureJavaOnlyJvm6Project", this)
publish()

pill {
    importAsLibrary = true
}

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
    classifier = "shadow"
    version = null
    callGroovy("manifestAttributes", manifest, project, "Main" /*true*/)

    exclude("**/*.proto")

    transform(KotlinModuleShadowTransformer(logger))

    configurations = listOf(embedded)
    relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
    relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    mergeServiceFiles()
}

val stripMetadata by tasks.creating {
    dependsOn("reflectShadowJar")
    val inputJar = reflectShadowJar.archivePath
    val outputJar = File("$libsDir/kotlin-reflect-stripped.jar")
    inputs.file(inputJar)
    outputs.file(outputJar)
    doLast {
        stripMetadata(logger, "kotlin/reflect/jvm/internal/impl/.*", inputJar, outputJar)
    }
}

val proguardOutput = "$libsDir/${property("archivesBaseName")}-proguard.jar"

val proguard by task<ProGuardTask> {
    dependsOn(stripMetadata)
    inputs.files(stripMetadata.outputs.files)
    outputs.file(proguardOutput)

    injars(mapOf("filter" to "!META-INF/versions/**"), stripMetadata.outputs.files)
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

val sourcesJar = sourcesJar(sourceSet = null) {
    dependsOn(relocateCoreSources)
    from(relocatedCoreSrc)
    from("$core/reflection.jvm/src")
}

val result by task<Jar> {
    dependsOn(proguard)
    from(zipTree(file(proguardOutput)))
    callGroovy("manifestAttributes", manifest, project, "Main")
}

val modularJar by task<Jar> {
    dependsOn(proguard)
    classifier = "modular"
    from(zipTree(file(proguardOutput)))
    from(zipTree(reflectShadowJar.archivePath)) {
        include("META-INF/versions/**")
    }
    callGroovy("manifestAttributes", manifest, project, "Main", true)
}

val dexMethodCount by task<DexMethodCount> {
    dependsOn(result)
    jarFile = result.outputs.files.single()
    ownPackages = listOf("kotlin.reflect")
}
tasks.getByName("check").dependsOn(dexMethodCount)

artifacts {
    listOf(mainJar.name, "runtime", "archives").forEach { configurationName ->
        add(
            configurationName,
            // idea can correctly import configurations of shadowJar as transitive dependencies which are required by tests
            if (kotlinBuildProperties.isInJpsBuildIdeaSync) reflectShadowJar else result
        )
    }

    add("archives", modularJar)
}

javadocJar()

dist(fromTask = result) {
    from(sourcesJar)
}
