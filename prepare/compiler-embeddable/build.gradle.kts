import org.gradle.kotlin.dsl.support.serviceOf

description = "Kotlin Compiler (embeddable)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

val testCompilationClasspath = configurations.create("testCompilationClasspath")
val testCompilerClasspath = configurations.create("testCompilerClasspath") {
    isCanBeConsumed = false
    extendsFrom(configurations["runtimeElements"])
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    runtimeOnly(project(":kotlin-daemon-embeddable"))
    runtimeOnly(libs.kotlinx.coroutines.core) { isTransitive = false }
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testCompilationClasspath(kotlinStdlib())
    testImplementation(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val runtimeJar = runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    exclude("META-INF/native-image/**")
    mergeServiceFiles()
    manifest {
        attributes("Multi-Release" to true)
    }
}

val sourcesJar = sourcesJar {
    val compilerTask = project(":kotlin-compiler").tasks.named<Jar>("sourcesJar")
    dependsOn(compilerTask)
    val archiveOperations = serviceOf<ArchiveOperations>()
    from(compilerTask.map { it.archiveFile }.map { archiveOperations.zipTree(it) })
}

val javadocJar = javadocJar {
    val compilerTask = project(":kotlin-compiler").tasks.named<Jar>("javadocJar")
    dependsOn(compilerTask)
    val archiveOperations = serviceOf<ArchiveOperations>()
    from(compilerTask.map { it.archiveFile }.map { archiveOperations.zipTree(it) })
}

publish {
    setArtifacts(listOf(runtimeJar, sourcesJar, javadocJar))
}

projectTests {
    testTask {
        dependsOn(runtimeJar)
        val testCompilerClasspathProvider = project.provider { testCompilerClasspath.asPath }
        val testCompilationClasspathProvider = project.provider { testCompilationClasspath.asPath }
        val runtimeJarPathProvider = project.provider { runtimeJar.get().outputs.files.asPath }
        doFirst {
            systemProperty(
                "compilerClasspath",
                "${runtimeJarPathProvider.get()}${File.pathSeparator}${testCompilerClasspathProvider.get()}"
            )
            systemProperty("compilationClasspath", testCompilationClasspathProvider.get())
        }
    }
}
