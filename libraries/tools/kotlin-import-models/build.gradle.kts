import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import javax.inject.Inject
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
}

configurations.embedded {
    isTransitive = false
}
configurations.compileOnly { extendsFrom(configurations.embedded) }
configurations.testImplementation { extendsFrom(configurations.embedded) }

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
// Replaces the default SBOM, because libraries packed into the JAR have to be listed as well
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

/**
 * Task that copies the resolved protoc executable to [protocExecutable] and makes it
 * executable, because the artifact in the dependency cache is not executable.
 */
abstract class SetupProtocTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val protocArtifact: ConfigurableFileCollection

    @get:OutputFile
    abstract val protocExecutable: RegularFileProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun setup() {
        val executable = protocExecutable.get().asFile
        fileSystemOperations.copy {
            from(protocArtifact.single())
            into(executable.parentFile)
            rename { executable.name }
            filePermissions { unix("rwxr-xr-x") }
        }
    }
}

/**
 * Task that unpacks the well-known schemas under `google/protobuf` from [protobufLibraries]
 * into [includeDirectory], so protoc can resolve the schema's `google/protobuf` imports.
 */
abstract class SetupProtobufIncludesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val protobufLibraries: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val includeDirectory: DirectoryProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun setup() {
        fileSystemOperations.sync {
            for (library in protobufLibraries) from(archiveOperations.zipTree(library))
            into(includeDirectory)
            include("google/protobuf/*.proto")
        }
    }
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

    val setupProtoc = tasks.register<SetupProtocTask>("setupProtoc") {
        protocArtifact.from(protoc)
        protocExecutable.set(layout.buildDirectory.file("protoc/bin/protoc"))
    }
    val setupProtobufIncludes = tasks.register<SetupProtobufIncludesTask>("setupProtobufIncludes") {
        protobufLibraries.from(protobufIncludes)
        includeDirectory.set(layout.buildDirectory.dir("protoc/include"))
    }

    // Regenerates the checked-in sources
    // TODO KT-90836 Add checks that the checked-in generated sources match the .proto file
    tasks.register<Exec>("protoc") {
        val protoSources = file("src/main/proto")
        val proto = file("src/main/proto/kotlin_import_models.proto")
        val javaOutput = file("src/generated/java")
        val kotlinOutput = file("src/generated/kotlin")

        val protocExecutable = setupProtoc.flatMap { it.protocExecutable }
        val protobufIncludeDirectory = setupProtobufIncludes.flatMap { it.includeDirectory }

        inputs.file(proto).withPropertyName("proto").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file(protocExecutable).withPropertyName("protocExecutable").withPathSensitivity(PathSensitivity.NAME_ONLY)
        inputs.dir(protobufIncludeDirectory).withPropertyName("protobufIncludes").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir(javaOutput)
        outputs.dir(kotlinOutput)

        doFirst {
            javaOutput.deleteRecursively()
            kotlinOutput.deleteRecursively()
            javaOutput.mkdirs()
            kotlinOutput.mkdirs()
        }

        workingDir(project.projectDir)
        executable = protocExecutable.get().asFile.absolutePath
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
