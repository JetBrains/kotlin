import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val shadows by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testCompile").extendsFrom(shadows)

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:light-classes"))
    shadows(project(":plugins:uast-kotlin-base"))

    // BEWARE: UAST should not depend on IJ platform so that it can work in Android Lint CLI mode (where IDE is not available)
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }

    implementation(project(":idea:idea-frontend-independent"))
    implementation(project(":idea-frontend-api"))
    implementation(project(":idea-frontend-fir"))

    testRuntime(project(":idea:idea-fir"))
    testImplementation(commonDep("junit:junit"))
    testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":idea:idea-test-framework"))
    testImplementation(projectTests(":idea"))
    testImplementation(projectTests(":idea:idea-fir"))
    testImplementation(projectTests(":plugins:uast-kotlin-base"))
    // To compare various aspects (e.g., render, log, type, value, etc.) against legacy UAST Kotlin
    testImplementation(projectTests(":plugins:uast-kotlin"))

    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

noDefaultJar()

runtimeJar(tasks.register<ShadowJar>("shadowJar")) {
    from(mainSourceSet.output)
    configurations = listOf(shadows)
}

testsJar ()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    val useFirIdeaPlugin = kotlinBuildProperties.useFirIdeaPlugin
    doFirst {
        if (!useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}
