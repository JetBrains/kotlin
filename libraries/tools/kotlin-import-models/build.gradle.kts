import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
}

val embedded = configurations.embedded.get().apply {
    isTransitive = false
    configurations.compileOnly.get().extendsFrom(this)
    configurations.testImplementation.get().extendsFrom(this)
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

    embedded(libs.protobuf.java)
    embedded(libs.protobuf.kotlin)

    api(kotlin("stdlib", coreDepsVersion))

    testImplementation(kotlin("test-junit5", coreDepsVersion))
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets.main.configure {
    java.srcDir("src/generated/java")
    java.srcDir("src/generated/kotlin")
}

publish(sbom = false)
// Libraries packed into the JAR have to be listed in the SBOM as well
configureSbom(gradleConfigurations = setOf("runtimeClasspath", "embedded"))
javadocJar()
sourcesJar()

val embeddableJar = runtimeJar(tasks.register<ShadowJar>("embeddable")) {
    from(mainSourceSet.output)
    exclude("**/*.proto")
    // Own prefix, because the shared org.jetbrains.kotlin.protobuf holds the compiler's patched Protobuf 2.6.1
    relocate(
        "com.google.protobuf",
        "org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf",
    )
}

projectTests {
    testTask()
}

// The dump describes the shipped jar, so generated signatures show the relocated runtime, which is not API
apiValidation {
    ignoredPackages += "org.jetbrains.kotlin.importmodels.internal"
}

tasks.apiBuild {
    inputJar.value(embeddableJar.flatMap { it.archiveFile })
}

run {
    val protoc = configurations.create("protoc") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    val protobufIncludes = configurations.create("protobufIncludes") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
    }

    dependencies {
        protobufIncludes(libs.protobuf.java)
        protoc(libs.protoc) {
            artifact {
                type = "exe"
                classifier = when (HostManager.hostOrNull) {
                    MACOS_ARM64 -> "osx-aarch_64"
                    MACOS_X64 -> "osx-x86_64"
                    MINGW_X64 -> "windows-x86_64"
                    LINUX_X64 -> "linux-x86_64"
                    else -> null
                }
            }
        }

        val protocVersion = libs.versions.protobuf.get()
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:linux-x86_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-aarch_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-x86_64@exe")
        implicitDependencies("com.google.protobuf:protoc:$protocVersion:windows-x86_64@exe")
    }

    val protocDirectory = layout.buildDirectory.dir("protoc/bin")
    val protobufIncludeDirectory = layout.buildDirectory.dir("protoc/include")
    val setupProtoc = tasks.register<Sync>("setupProtoc") {
        from(protoc)
        into(protocDirectory)
        rename { "protoc" }
        filePermissions { unix("rwxr-xr-x") }
    }
    val setupProtobufIncludes = tasks.register<Sync>("setupProtobufIncludes") {
        from({ protobufIncludes.map { zipTree(it) } }) {
            include("google/protobuf/*.proto")
        }
        into(protobufIncludeDirectory)
    }

    // Regenerates the checked-in sources
    // TODO KT-90836 Add checks that the checked-in generated sources match the .proto file
    tasks.register<Exec>("protoc") {
        val protoSources = file("src/main/proto")
        val proto = file("src/main/proto/kotlin_import_models.proto")
        val javaOutput = file("src/generated/java")
        val kotlinOutput = file("src/generated/kotlin")

        inputs.file(proto)
        inputs.files(setupProtoc).withPropertyName("protocExecutable")
        inputs.files(setupProtobufIncludes).withPropertyName("protobufIncludes")
        outputs.dir(javaOutput)
        outputs.dir(kotlinOutput)

        doFirst {
            javaOutput.deleteRecursively()
            kotlinOutput.deleteRecursively()
            javaOutput.mkdirs()
            kotlinOutput.mkdirs()
        }

        workingDir(project.projectDir)
        executable = protocDirectory.get().file("protoc").asFile.absolutePath
        argumentProviders.add {
            listOf(
                "-I=$protoSources",
                "-I=${protobufIncludeDirectory.get().asFile.absolutePath}",
                "--java_out=${javaOutput.absolutePath}",
                "--kotlin_out=${kotlinOutput.absolutePath}",
                proto.path,
            )
        }
    }
}
