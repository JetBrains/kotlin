import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext

// NOTE: Rename JvmPackageTable to ModuleProtoBuf upon the next update of the bootstrap compiler
import org.jetbrains.kotlin.serialization.jvm.JvmPackageTable

import proguard.gradle.ProGuardTask
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

description = "Kotlin Full Reflection Library"

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("net.sf.proguard:proguard-gradle:${property("versions.proguard")}")
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins { java }

callGroovy("configureJavaOnlyJvm6Project", this)
publish()

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
    compile(project(":kotlin-stdlib"))

    proguardDeps(project(":kotlin-stdlib"))
    proguardDeps(files(firstFromJavaHomeThatExists("lib/rt.jar", "../Classes/classes.jar")))

    shadows(project(":kotlin-reflect-api"))
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

        val input = DataInputStream(context.`is`)
        val version = IntArray(input.readInt()) { input.readInt() }
        logger.info("Transforming ${context.path} with version ${version.toList()}")

        val table = JvmPackageTable.PackageTable.parseFrom(context.`is`).toBuilder()

        val newTable = JvmPackageTable.PackageTable.newBuilder().apply {
            for (packageParts in table.packagePartsList + table.metadataPartsList) {
                addPackageParts(JvmPackageTable.PackageParts.newBuilder(packageParts).apply {
                    packageFqName = relocate(packageFqName)
                })
            }
            addAllJvmPackageName(table.jvmPackageNameList.map(::relocate))
        }

        val baos = ByteArrayOutputStream()
        val output = DataOutputStream(baos)
        output.writeInt(version.size)
        version.forEach(output::writeInt)
        newTable.build().writeTo(output)
        output.flush()

        data += Entry(context.path, baos.toByteArray())
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
    callGroovy("manifestAttributes", manifest, project, "Main")

    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from(project(":core:descriptors.jvm").the<JavaPluginConvention>().sourceSets.getByName("main").resources) {
        include("META-INF/services/**")
    }
    from(project(":core:deserialization").the<JavaPluginConvention>().sourceSets.getByName("main").resources) {
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

val mainArchiveName = "${property("archivesBaseName")}-$version.jar"
val outputJarPath = "$libsDir/$mainArchiveName"

val proguard by task<ProGuardTask> {
    dependsOn(stripMetadata)
    inputs.files(stripMetadata.outputs.files)
    outputs.file(outputJarPath)

    injars(stripMetadata.outputs.files)
    outjars(outputJarPath)

    libraryjars(proguardDeps)

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

val result = proguard

val dexMethodCount by task<DexMethodCount> {
    dependsOn(result)
    jarFile = File(outputJarPath)
    ownPackages = listOf("kotlin.reflect")
}
tasks.getByName("check").dependsOn(dexMethodCount)

artifacts {
    val artifactJar = mapOf(
            "file" to File(outputJarPath),
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
