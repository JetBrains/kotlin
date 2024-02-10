import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.kotlinNativeDist

plugins {
    kotlin("jvm")
}

description = "Embeddable JAR of Kotlin/Native compiler"
group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

val kotlinNativeEmbedded by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val kotlinNativeSources by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
    }
}

val kotlinNativeJavadoc by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
    }
}

dependencies {
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Runtime"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Indexer"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:StubGenerator"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Skia"))
    kotlinNativeEmbedded(project(":kotlin-native:backend.native"))
    kotlinNativeEmbedded(project(":kotlin-native:utilities:cli-runner"))
    kotlinNativeEmbedded(project(":kotlin-native:klib"))
    kotlinNativeEmbedded(project(":kotlin-native:backend.native", "cli_bcApiElements"))
    kotlinNativeEmbedded(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))
    kotlinNativeEmbedded(project(":kotlin-compiler")) { isTransitive = false }

    kotlinNativeSources(project(":kotlin-native:backend.native"))
    kotlinNativeJavadoc(project(":kotlin-native:backend.native"))

    testImplementation(libs.junit4)
    testImplementation(kotlinTest("junit"))
}

val compiler = embeddableCompiler("kotlin-native-compiler-embeddable") {
    from(kotlinNativeEmbedded)
    mergeServiceFiles()
}

val runtimeJar = runtimeJar(compiler) {
    exclude("com/sun/jna/**")
    mergeServiceFiles()
}

val archiveZipper = serviceOf<ArchiveOperations>()::zipTree

val sourcesJar = sourcesJar {
    dependsOn(kotlinNativeSources)
    from { kotlinNativeSources.map { archiveZipper(it) } }
}

val javadocJar = javadocJar {
    dependsOn(kotlinNativeJavadoc)
    from { kotlinNativeJavadoc.map { archiveZipper(it) } }
}

publish {
    setArtifacts(listOf(runtimeJar, sourcesJar, javadocJar))
}

sourceSets {
    "main" {}
    "test" {
        kotlin {
            srcDir("tests/kotlin")
        }
    }
}

projectTest {
    /**
     * It's expected that test should be executed on CI, but currently this project under `kotlin.native.enabled`
     */
    dependsOn(runtimeJar)
    val runtimeJarPathProvider = project.provider {
        val jar = runtimeJar.get().outputs.files.asPath
        val trove = configurations.detachedConfiguration(
                dependencies.create(commonDependency("org.jetbrains.intellij.deps:trove4j"))
        )
        (trove.files + jar).joinToString(File.pathSeparatorChar.toString())
    }
    doFirst {
        systemProperty("compilerClasspath", runtimeJarPathProvider.get())
        systemProperty("kotlin.native.home", kotlinNativeDist)
    }
}