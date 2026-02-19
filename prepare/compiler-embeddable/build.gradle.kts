import org.gradle.kotlin.dsl.support.serviceOf

description = "Kotlin Compiler (embeddable)"

plugins {
    kotlin("jvm")
    id("project-tests-convention")
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
    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    runtimeOnly(project(":kotlin-daemon-embeddable"))
    runtimeOnly(libs.kotlinx.coroutines.core) { isTransitive = false }
    testImplementation(libs.junit4)
    testImplementation(kotlinTest("junit"))
    testCompilationClasspath(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val runtimeJar = runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    mergeServiceFiles()
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
    testTask(jUnitMode = JUnitMode.JUnit4) {
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

