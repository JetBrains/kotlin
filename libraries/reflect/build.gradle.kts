import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import kotlinx.metadata.jvm.KmModuleVisitor
import kotlinx.metadata.jvm.KotlinModuleMetadata
import org.gradle.kotlin.dsl.support.serviceOf
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

description = "Kotlin Full Reflection Library"

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.2.0")
    }
}

plugins {
    java
}

val JDK_16: String by rootProject.extra

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

    embedded(project(":kotlin-reflect-api"))
    embedded(project(":core:metadata"))
    embedded(project(":core:metadata.jvm"))
    embedded(project(":core:compiler.common"))
    embedded(project(":core:compiler.common.jvm"))
    embedded(project(":core:deserialization.common"))
    embedded(project(":core:deserialization.common.jvm"))
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

    exclude("**/*.proto")
    exclude("org/jetbrains/annotations/Nls*.class")

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

    inputs.file(inputJar).withNormalizer(ClasspathNormalizer::class.java)

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

val proguard by task<CacheableProguardTask> {
    dependsOn(stripMetadata)

    injars(mapOf("filter" to "!META-INF/versions/**"), stripMetadata.get().outputs.files)
    injars(mapOf("filter" to "!META-INF/**,!**/*.kotlin_builtins"), proguardAdditionalInJars)
    outjars(proguardOutput)

    jdkHome = File(JDK_16)
    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardDeps)
    libraryjars(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar", jdkHome = jdkHome!!))

    configuration("$core/reflection.jvm/reflection.pro")
}

val relocateCoreSources by task<Copy> {
    val relocatedCoreSrc = relocatedCoreSrc
    val fs = serviceOf<FileSystemOperations>()
    doFirst {
        fs.delete {
            delete(relocatedCoreSrc)
        }
    }

    from("$core/descriptors/src")
    from("$core/descriptors.common/src")
    from("$core/descriptors.jvm/src")
    from("$core/descriptors.runtime/src")
    from("$core/deserialization/src")
    from("$core/deserialization/deserialization.common/src")
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

    outputs.cacheIf { true }
}

noDefaultJar()

java {
    withSourcesJar()
}

configurePublishedComponent {
    addVariantsFromConfiguration(configurations[JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME]) { }
}

val sourcesJar = tasks.named<Jar>("sourcesJar") {
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
        zipTree(intermediate.get().singleOutputFile())
    }
    from(zipTree(provider { reflectShadowJar.get().archiveFile.get().asFile })) {
        include("META-INF/versions/**")
    }
    callGroovy("manifestAttributes", manifest, project, "Main", true)
}

javadocJar()

modularJar {
    dependsOn(intermediate)
    from {
        zipTree(intermediate.get().singleOutputFile())
    }
    from(zipTree(provider { reflectShadowJar.get().archiveFile.get().asFile })) {
        include("META-INF/versions/**")
    }
    callGroovy("manifestAttributes", manifest, project, "Main", true)
}

dexMethodCount {
    dependsOn(result)
    jarFile = result.get().outputs.files.single()
    ownPackages = listOf("kotlin.reflect")
}

artifacts {
    listOf(mainJar.name, "runtime", "archives", "runtimeElements").forEach { configurationName ->
        add(configurationName, result.get().outputs.files.singleFile) {
            builtBy(result)
        }
    }
}
