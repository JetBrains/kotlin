import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.kotlinNativeDist

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

repositories{
    mavenCentral()
}

val kotlinNativeEmbedded by configurations.creating
val testPlugin by configurations.creating
val testPluginRuntime by configurations.creating
fun DependencyHandlerScope.kotlinNativeEmbaded(any: Any) = add(kotlinNativeEmbedded.name, any)
fun DependencyHandlerScope.testPlugin(any: Any) = add(testPlugin.name, any)

// org.jetbrains.kotlinx:kotlinx-serialization-json-linuxx64:1.2.1
val pluginRuntimePlatform = Regex("^.*-(.*):[\\.\\d]+")
fun DependencyHandlerScope.testPluginRuntime(any: Any) {
    val notation = any as? String ?: return add(testPluginRuntime.name, any){}
    val (group, artifact, version) = notation.split(":")
    val platformName = HostManager.host.name
    val gradlePlatformName = platformName.replace("_", "")
    return add(testPluginRuntime.name, "$group:$artifact-$gradlePlatformName:$version") {
        isTransitive = false
        attributes {
            attribute(Attribute.of("artifactType", String::class.java), "org.jetbrains.kotlin.klib")
            attribute(Attribute.of("org.gradle.status", String::class.java), "release")
            attribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java), platformName)
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "native")
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        }
    }
}

dependencies {
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Runtime"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Indexer"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:StubGenerator"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Skia"))
    kotlinNativeEmbedded(project(":kotlin-native:backend.native"))
    kotlinNativeEmbedded(project(":kotlin-native:utilities:cli-runner"))
    kotlinNativeEmbedded(project(":kotlin-native:utilities:basic-utils"))
    kotlinNativeEmbedded(project(":kotlin-native:klib"))
    kotlinNativeEmbedded(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testPlugin(project(":kotlin-serialization"))

    testPluginRuntime("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    testPluginRuntime("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
}

val compiler = embeddableCompiler("kotlin-native-compiler-embeddable") {
    from(kotlinNativeEmbedded)
}

val runtimeJar = runtimeJar(compiler) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    exclude("org/jetbrains/kotlin/cli/jvm/**")
    mergeServiceFiles()
}


kotlin.sourceSets["test"].kotlin.srcDir("tests/kotlin")


projectTest {
    dependsOn(runtimeJar)
    val testCompilerClasspathProvider = project.provider { runtimeJar.get().outputs.files.asPath }
    val runtimeJarPathProvider = project.provider { runtimeJar.get().outputs.files.asPath }
    doFirst {
        systemProperty("compilerClasspath", "${runtimeJarPathProvider.get()}${File.pathSeparator}${testCompilerClasspathProvider.get()}")
        systemProperty("kotlin.native.home", kotlinNativeDist)
        configurations.findByName("testPlugin")?.files?.single { it.name.startsWith("kotlin-serialization") }?.let {
                systemProperty("plugin", it.absolutePath)
        } ?: failFast
        configurations.findByName("testPluginRuntime")?.files?.forEachIndexed { index, it ->
                systemProperty("runtime.$index", it.absolutePath)
        }

    }
}


