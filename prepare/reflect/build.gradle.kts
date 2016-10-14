
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(files("$rootDir/ideaSDK/lib/asm-all.jar"))
    }
}

repositories {
    mavenCentral()
}

apply { plugin("com.github.johnrengelman.shadow") }

// Set to false to prevent relocation and metadata stripping on kotlin-reflect.jar and reflection sources. Use to debug reflection
val obfuscateReflect = true

val packReflectCfg = configurations.create("packed-reflect")

val outputReflectJarFileBase = "$buildDir/libs/kotlin-reflect"

artifacts.add("packed-reflect", File(outputReflectJarFileBase + ".jar"))

dependencies {
    "packed-reflect"(project(":core")) { isTransitive = false }
    "packed-reflect"(project(":core.reflection")) { isTransitive = false }
    "packed-reflect"(project(":custom-dependencies:protobuf-lite", configuration = "protobuf-lite")) { isTransitive = false }
    "packed-reflect"("javax.inject:javax.inject:1")
    "packed-reflect"(project(":prepare:build.version", configuration = "default"))
}

val prePackReflectTask = task<ShadowJar>("pre-pack-reflect") {
    classifier = if (obfuscateReflect) outputReflectJarFileBase + "_beforeStrip" else outputReflectJarFileBase
    configurations = listOf(packReflectCfg)
    from(packReflectCfg.files)
    from(project(":core").file("descriptor.loader.java/src")) {
        include("META-INF/services/**")
    }
    manifest.attributes.put("Class-Path", "kotlin-runtime.jar")

    if (obfuscateReflect) {
        relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
        relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    }
}

task("pack-reflect") {
    dependsOn(prePackReflectTask)
    val inFile = File(outputReflectJarFileBase + "_beforeStrip.jar")
    val outFile = File(outputReflectJarFileBase + ".jar")
    val annotationRegex = "kotlin/Metadata".toRegex()
    val classRegex = "kotlin/reflect/jvm/internal/impl/.*".toRegex()
    doLast {
        println("Stripping annotations from all classes in $inFile")
        println("Input file size: ${inFile.length()} bytes")

        fun transform(entryName: String, bytes: ByteArray): ByteArray {
            if (!entryName.endsWith(".class")) return bytes
            if (!classRegex.matches(entryName.removeSuffix(".class"))) return bytes

            var changed = false
            val classWriter = ClassWriter(0)
            val classVisitor = object : ClassVisitor(Opcodes.ASM5, classWriter) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    if (annotationRegex.matches(Type.getType(desc).getInternalName())) {
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

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use {
            outJar ->
            val inJar = JarFile(inFile)
            try {
                for (entry in inJar.entries()) {
                    val inBytes = inJar.getInputStream(entry).readBytes()
                    val outBytes = transform(entry.getName(), inBytes)

                    if (inBytes.size < outBytes.size) {
                        error("Size increased for ${entry.getName()}: was ${inBytes.size} bytes, became ${outBytes.size} bytes")
                    }

                    entry.setCompressedSize(-1L)
                    outJar.putNextEntry(entry)
                    outJar.write(outBytes)
                    outJar.closeEntry()
                }
            }
            finally {
                // Yes, JarFile does not extend Closeable on JDK 6 so we can't use "use" here
                inJar.close()
            }
        }

        println("Output written to $outFile")
        println("Output file size: ${outFile.length()} bytes")
    }
}

defaultTasks("pack-reflect")

