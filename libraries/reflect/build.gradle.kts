import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.pill.PillExtension
import proguard.gradle.ProGuardTask
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import kotlinx.metadata.jvm.KotlinModuleMetadata
import kotlinx.metadata.jvm.KmModuleVisitor

description = "Kotlin Full Reflection Library"

buildscript {
    repositories {
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.0.2")
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
val annotationsSrc = "$buildDir/annotations"
val relocatedCoreSrc = "$buildDir/core-relocated"
val libsDir = property("libsDir")

sourceSets {
    "main" {
        java.srcDir(annotationsSrc)
    }
}

val proguardDeps by configurations.creating
val shadows by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
val mainJar by configurations.creating

dependencies {
    compile(projectDist(":kotlin-stdlib"))

    proguardDeps(project(":kotlin-stdlib"))
    proguardDeps(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar", jdkHome = File(property("JDK_16") as String))))

    shadows(project(":kotlin-reflect-api"))
    shadows(project(":core:metadata"))
    shadows(project(":core:metadata.jvm"))
    shadows(project(":core:descriptors"))
    shadows(project(":core:descriptors.jvm"))
    shadows(project(":core:deserialization"))
    shadows(project(":core:descriptors.runtime"))
    shadows(project(":core:util.runtime"))
    shadows("javax.inject:javax.inject:1")
    shadows(project(":custom-dependencies:protobuf-lite", configuration = "default"))
}

val copyAnnotations by task<Sync> {
    // copy just two missing annotations
    from("$core/runtime.jvm/src") {
        include("**/Mutable.java")
        include("**/ReadOnly.java")
    }
    into(annotationsSrc)
    includeEmptyDirs = false
}

tasks.getByName("compileJava").dependsOn(copyAnnotations)

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

    override fun modifyOutputStream(os: ZipOutputStream) {
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
    callGroovy("manifestAttributes", manifest, project, "Main", true)

    from(javaPluginConvention().sourceSets.getByName("main").output)
    from(project(":core:descriptors.jvm").javaPluginConvention().sourceSets.getByName("main").resources) {
        include("META-INF/services/**")
    }
    from(project(":core:deserialization").javaPluginConvention().sourceSets.getByName("main").resources) {
        include("META-INF/services/**")
    }

    exclude("**/*.proto")

    transform(KotlinModuleShadowTransformer(logger))

    configurations = listOf(shadows)
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
    val artifactJar = mapOf(
        "file" to result.outputs.files.single(),
        "builtBy" to result,
        "name" to property("archivesBaseName")
    )

    add(mainJar.name, artifactJar)
    add("runtime", artifactJar)
    add("archives", artifactJar)
}

javadocJar()

dist(fromTask = result) {
    from(sourcesJar)
}
