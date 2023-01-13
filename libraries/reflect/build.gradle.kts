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
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")
    }
}

plugins {
    `java-library`
}

configureJavaOnlyToolchain(JdkMajorVersion.JDK_1_8)

publish()

val core = "$rootDir/core"
val relocatedCoreSrc = "$buildDir/core-relocated"

val proguardDeps by configurations.creating
val proguardAdditionalInJars by configurations.creating

val embedded by configurations
embedded.isTransitive = false

configurations.getByName("compileOnly").extendsFrom(embedded)

dependencies {
    api(kotlinStdlib())

    proguardDeps(kotlinStdlib())
    proguardAdditionalInJars(project(":kotlin-annotations-jvm"))

    embedded(project(":kotlin-reflect-api")) { isTransitive = false }
    embedded(project(":core:metadata")) { isTransitive = false }
    embedded(project(":core:metadata.jvm")) { isTransitive = false }
    embedded(project(":core:compiler.common")) { isTransitive = false }
    embedded(project(":core:compiler.common.jvm")) { isTransitive = false }
    embedded(project(":core:deserialization.common")) { isTransitive = false }
    embedded(project(":core:deserialization.common.jvm")) { isTransitive = false }
    embedded(project(":core:descriptors")) { isTransitive = false }
    embedded(project(":core:descriptors.jvm")) { isTransitive = false }
    embedded(project(":core:deserialization")) { isTransitive = false }
    embedded(project(":core:descriptors.runtime")) { isTransitive = false }
    embedded(project(":core:util.runtime")) { isTransitive = false }
    embedded("javax.inject:javax.inject:1") { isTransitive = false }
    embedded(protobufLite()) { isTransitive = false }

    compileOnly("org.jetbrains:annotations:13.0")
}

@CacheableTransformer
class KotlinModuleShadowTransformer(private val logger: Logger, private val useK2: Boolean) : Transformer {
    @Suppress("ArrayInDataClass")
    private data class Entry(val path: String, val bytes: ByteArray)

    private val data = mutableListOf<Entry>()

    override fun getName() = "KotlinModuleShadowTransformer"

    override fun canTransformResource(element: FileTreeElement): Boolean =
        element.path.substringAfterLast(".") == KOTLIN_MODULE

    override fun transform(context: TransformerContext) {
        fun relocate(content: String): String =
            context.relocators.fold(content) { acc, relocator -> relocator.applyToSourceContent(acc) }

        val writer = KotlinModuleMetadata.Writer()
        logger.info("Transforming ${context.path}")
        if (useK2) {
            // TODO: remove this branch after migration to version 1.9
            val internalData = org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping.loadModuleMapping(
                context.`is`.readBytes(), javaClass.name, skipMetadataVersionCheck = true, isJvmPackageNameSupported = true
            ) {
            }
            val visitor = object : KmModuleVisitor(writer) {
                override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
                    assert(multiFileClassParts.isEmpty()) { multiFileClassParts } // There are no multi-file class parts in core
                    super.visitPackageParts(relocate(fqName), fileFacades.map(::relocate), multiFileClassParts)
                }
            }
            for ((fqName, parts) in internalData.packageFqName2Parts) {
                val (fileFacades, multiFileClassParts) = parts.parts.partition { parts.getMultifileFacadeName(it) == null }
                visitor.visitPackageParts(fqName, fileFacades, multiFileClassParts.associateWith { parts.getMultifileFacadeName(it)!! })
            }
            visitor.visitEnd()
        } else {
            val metadata = KotlinModuleMetadata.read(context.`is`.readBytes())
                ?: error("Not a .kotlin_module file: ${context.path}")
            // TODO: writer declaration and logger.info call from above should be move here after migration to version 1.9
            metadata.accept(object : KmModuleVisitor(writer) {
                override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
                    assert(multiFileClassParts.isEmpty()) { multiFileClassParts } // There are no multi-file class parts in core
                    super.visitPackageParts(relocate(fqName), fileFacades.map(::relocate), multiFileClassParts)
                }
            })
        }

        data += Entry(context.path, writer.write().bytes)
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

val reflectShadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    configurations = listOf(embedded)

    exclude("**/*.proto")
    exclude("org/jetbrains/annotations/Nls*.class")

    if (kotlinBuildProperties.relocation) {
        mergeServiceFiles()
        transform(KotlinModuleShadowTransformer(logger, project.kotlinBuildProperties.useFir))
        relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
        relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    }
}

val stripMetadata by tasks.registering {
    dependsOn(reflectShadowJar)
    val inputJar = provider { reflectShadowJar.get().outputs.files.singleFile }
    val outputJar = fileFrom(base.libsDirectory.asFile.get(), "${base.archivesName.get()}-$version-stripped.jar")

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

val proguard by task<CacheableProguardTask> {
    dependsOn(stripMetadata)

    injars(mapOf("filter" to "!META-INF/versions/**"), stripMetadata.get().outputs.files)
    injars(mapOf("filter" to "!META-INF/**,!**/*.kotlin_builtins"), proguardAdditionalInJars)
    outjars(fileFrom(base.libsDirectory.asFile.get(), "${base.archivesName.get()}-$version-proguard.jar"))

    javaLauncher.set(project.getToolchainLauncherFor(chooseJdk_1_8ForJpsBuild(JdkMajorVersion.JDK_1_8)))
    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardDeps)
    libraryjars(
        project.files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )
            }
        )
    )

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
    filter(org.apache.tools.ant.filters.FixCrLfFilter::class, "eol" to org.apache.tools.ant.filters.FixCrLfFilter.CrLf.newInstance("lf"))

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

dexMethodCount {
    dependsOn(result)
    jarFile = result.get().outputs.files.single()
    ownPackages.set(listOf("kotlin.reflect"))
}

artifacts {
    listOf("archives", "runtimeElements").forEach { configurationName ->
        add(configurationName, provider { result.get().outputs.files.singleFile }) {
            builtBy(result)
        }
    }
}
