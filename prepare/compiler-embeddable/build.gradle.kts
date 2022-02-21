import java.util.stream.Collectors
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.kotlin.dsl.support.serviceOf
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
    runtimeOnly(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
    Platform[203].orHigher {
        runtimeOnly(commonDependency("net.java.dev.jna", "jna"))
    }
    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testCompilationClasspath(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

publish()

// dummy is used for rewriting dependencies to the shaded packages in the embeddable compiler
compilerDummyJar(compilerDummyForDependenciesRewriting("compilerDummy") {
    archiveClassifier.set("dummy")
})


val runtimeJar = runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    mergeServiceFiles()
}

sourcesJar {
    val compilerTask = project(":kotlin-compiler").tasks.named<Jar>("sourcesJar")
    dependsOn(compilerTask)
    // workaround for https://github.com/gradle/gradle/issues/17936
    val archiveOperations = serviceOf<ArchiveOperations>()
    val sourcesJar by lazy { compilerTask.get().archiveFile.get() }
    from({ archiveOperations.zipTree(sourcesJar) })
}

javadocJar {
    val compilerTask = project(":kotlin-compiler").tasks.named<Jar>("javadocJar")
    dependsOn(compilerTask)
    // workaround for https://github.com/gradle/gradle/issues/17936
    val archiveOperations = serviceOf<ArchiveOperations>()
    val javadocJarFile by lazy { compilerTask.get().archiveFile.get() }
    from({ archiveOperations.zipTree(javadocJarFile) })
}

projectTest {
    dependsOn(runtimeJar)
    val testCompilerClasspathProvider = project.provider { testCompilerClasspath.asPath }
    val testCompilationClasspathProvider = project.provider { testCompilationClasspath.asPath }
    val runtimeJarPathProvider = project.provider { runtimeJar.get().outputs.files.asPath }
    doFirst {
        systemProperty("compilerClasspath", "${runtimeJarPathProvider.get()}${File.pathSeparator}${testCompilerClasspathProvider.get()}")
        systemProperty("compilationClasspath", testCompilationClasspathProvider.get())
    }
}

