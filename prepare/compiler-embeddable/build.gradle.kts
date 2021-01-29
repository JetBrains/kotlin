import java.util.stream.Collectors
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipOutputStream

description = "Kotlin Compiler (embeddable)"

plugins {
    kotlin("jvm")
}

val testCompilationClasspath by configurations.creating
val testCompilerClasspath by configurations.creating {
    isCanBeConsumed = false
    extendsFrom(configurations["runtimeElements"])
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(project(":kotlin-reflect"))
    runtimeOnly(project(":kotlin-daemon-embeddable"))
    runtimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompilationClasspath(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

publish()

noDefaultJar()

// dummy is used for rewriting dependencies to the shaded packages in the embeddable compiler
compilerDummyJar(compilerDummyForDependenciesRewriting("compilerDummy") {
    archiveClassifier.set("dummy")
})

class CoreXmlShadingTransformer : Transformer {
    companion object {
        private const val XML_NAME = "META-INF/extensions/core.xml"
    }

    private val content = StringBuilder()

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return (element.name == XML_NAME)
    }

    override fun transform(context: TransformerContext) {
        val text = context.`is`.bufferedReader().lines()
            .map { it.replace("com.intellij.psi", "org.jetbrains.kotlin.com.intellij.psi") }
            .collect(Collectors.joining("\n"))
        content.appendln(text)
        context.`is`.close()
    }

    override fun hasTransformedResource(): Boolean {
        return content.isNotEmpty()
    }

    override fun modifyOutputStream(outputStream: ZipOutputStream, preserveFileTimestamps: Boolean) {
        val entry = ZipEntry(XML_NAME)
        outputStream.putNextEntry(entry)
        outputStream.write(content.toString().toByteArray())
    }
}

val runtimeJar = runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    mergeServiceFiles()

    transform(CoreXmlShadingTransformer::class.java)
}

sourcesJar()
javadocJar()

projectTest {
    dependsOn(runtimeJar)
    doFirst {
        systemProperty("compilerClasspath", "${runtimeJar.get().outputs.files.asPath}${File.pathSeparator}${testCompilerClasspath.asPath}")
        systemProperty("compilationClasspath", testCompilationClasspath.asPath)
    }
}


