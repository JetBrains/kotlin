description = "Kotlin compiler client embeddable"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

val testCompilerClasspath = configurations.create("testCompilerClasspath") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

val testCompilationClasspath = configurations.create("testCompilationClasspath")

dependencies {
    embedded(project(":compiler:cli-base")) { isTransitive = false }
    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(project(":kotlin-daemon-client")) { isTransitive = false }
    
    testImplementation(project(":compiler:cli-base"))
    testImplementation(project(":daemon-common"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testCompilerClasspath(project(":kotlin-compiler"))
    testCompilerClasspath(project(":kotlin-scripting-compiler"))
    testCompilerClasspath(project(":kotlin-daemon"))
    testCompilationClasspath(kotlinStdlib())
    testCompilationClasspath(project(":kotlin-script-runtime"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTests {
    testTask {
        dependsOn(":kotlin-compiler:jar")
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        val testCompilerClasspathProvider = project.provider { testCompilerClasspath.asPath }
        val testCompilationClasspathProvider = project.provider { testCompilationClasspath.asPath }
        doFirst {
            systemProperty("compilerClasspath", testCompilerClasspathProvider.get())
            systemProperty("compilationClasspath", testCompilationClasspathProvider.get())
        }
    }
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
