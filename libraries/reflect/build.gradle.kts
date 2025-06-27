import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Full Reflection Library"

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    }
}

plugins {
    kotlin("jvm")
}

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

sourceSets {
    "main" {
        java.srcDir("$rootDir/core/reflection.jvm/src")
        resources.srcDir("$rootDir/core/reflection.jvm/resources")
    }
    if (kotlinBuildProperties.includeJava9) {
        "java9" {
            java.srcDir("$rootDir/libraries/reflect/api/src/java9/java")
        }
    }
}

publish()

val core = "$rootDir/core"
val relocatedCoreSrc = "${layout.buildDirectory.get().asFile}/core-relocated"

val proguardDeps by configurations.creating
val proguardAdditionalInJars by configurations.creating

val embedded by configurations
embedded.isTransitive = false

configurations.getByName("compileOnly").extendsFrom(embedded)

dependencies {
    api(kotlinStdlib())

    proguardDeps(kotlinStdlib())
    proguardAdditionalInJars(project(":kotlin-annotations-jvm"))

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
    embedded(protobufFull()) { isTransitive = false }

    compileOnly("org.jetbrains:annotations:13.0")

    // Declaring kotlin-metadata-jvm dependency as `embedded` is undesirable because it leads to protobuf-generated classes packed twice in
    // the resulting jar. So we declare it as `compileOnly` and pack its output manually in the shadow configuration.
    // Also, we need to compile against the unshaded configuration to avoid having incompatible (located in different packages)
    // protobuf-generated classes because then we would not be able to pass protobuf obtained from descriptors to kotlin-metadata.
    compileOnly(project(":kotlin-metadata-jvm", "unshaded"))
    compileOnly(project(":kotlin-metadata"))
}

if (kotlinBuildProperties.includeJava9) {
    val java9PatchModule = configurations.register("java9PatchModule") {
        extendsFrom(configurations.getByName("compileOnly"))
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        isCanBeResolved = true
    }
    configureJava9Compilation(
        "kotlin.reflect",
        listOf(sourceSets["main"].output, java9PatchModule.get()),
    )
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.set(
            listOf(
                "-Xallow-kotlin-package",
                "-Xno-new-java-annotation-targets",
                "-Xdont-warn-on-error-suppression",
            )
        )
        moduleName.set("kotlin-reflection")
    }
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

val reflectShadowJar by task<ShadowJar> {
    archiveClassifier.set("shadow")
    configurations = listOf(embedded)

    from(sourceSets["main"].output)
    if (kotlinBuildProperties.includeJava9) {
        from(sourceSets["java9"].output)
    }
    from(project(":kotlin-metadata").sourceSets["main"].output) {
        exclude("META-INF/metadata.kotlin_module")
    }
    from(project(":kotlin-metadata-jvm").sourceSets["main"].output) {
        exclude("META-INF/metadata.jvm.kotlin_module")
    }
    exclude("**/*.proto")
    exclude("org/jetbrains/annotations/Nls*.class")

    if (kotlinBuildProperties.relocation) {
        mergeServiceFiles()
        transform(KotlinModuleShadowTransformer(logger))
        relocate("kotlin.metadata", "kotlin.reflect.jvm.internal.impl.km")
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
                )!!
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
        zipTree(intermediate.get().singleOutputFile(layout))
    }
    from(zipTree(provider { reflectShadowJar.get().archiveFile.get().asFile })) {
        include("META-INF/versions/**")
    }
    includeEmptyDirs = false
    manifestAttributes(
        manifest,
        component = "Main",
        multiRelease = true
    )
}

javadocJar()

dexMethodCount {
    jarFile.fileProvider(result.map { it.outputs.files.singleFile })
    ownPackages.set(listOf("kotlin.reflect"))
}

artifacts {
    listOf("archives", "runtimeElements").forEach { configurationName ->
        add(configurationName, result.map { it.outputs.files.singleFile })
    }
}
