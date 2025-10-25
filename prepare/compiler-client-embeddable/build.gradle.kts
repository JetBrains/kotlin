description = "Kotlin compiler client embeddable"

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

val testCompilerClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

val testCompilationClasspath by configurations.creating

dependencies {
    embedded(project(":compiler:cli-common")) { isTransitive = false }
    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(project(":kotlin-daemon-client")) { isTransitive = false }
    
    testImplementation(project(":compiler:cli-common"))
    testImplementation(project(":daemon-common"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(libs.junit4)
    testImplementation(kotlinTest("junit"))
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
    testTask(jUnitMode = JUnitMode.JUnit4) {
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
