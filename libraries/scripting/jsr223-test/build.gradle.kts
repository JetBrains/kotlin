
plugins {
    kotlin("jvm")
}

val embeddableTestRuntime by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

val testJsr223Runtime by configurations.creating {
    extendsFrom(configurations["testRuntimeClasspath"])
}

val testCompilationClasspath by configurations.creating

dependencies {
    testImplementation(libs.junit4)
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":core:util.runtime"))

    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }

    testRuntimeOnly(project(":kotlin-scripting-jsr223-unshaded"))
    testRuntimeOnly(project(":kotlin-compiler"))

    embeddableTestRuntime(libs.junit4)
    embeddableTestRuntime(project(":kotlin-scripting-jsr223"))
    embeddableTestRuntime(project(":kotlin-scripting-compiler-embeddable"))
    embeddableTestRuntime(testSourceSet.output)

    testCompilationClasspath(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    val testRuntimeProvider = project.provider { testJsr223Runtime.asPath }
    val testCompilationClasspathProvider = project.provider { testCompilationClasspath.asPath }
    doFirst {
        systemProperty("testJsr223RuntimeClasspath", testRuntimeProvider.get())
        systemProperty("testCompilationClasspath", testCompilationClasspathProvider.get())
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}

projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime
    val testRuntimeProvider = project.provider { embeddableTestRuntime.asPath }
    val testCompilationClasspathProvider = project.provider { testCompilationClasspath.asPath }
    doFirst {
        systemProperty("testJsr223RuntimeClasspath", testRuntimeProvider.get())
        systemProperty("testCompilationClasspath", testCompilationClasspathProvider.get())
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}
