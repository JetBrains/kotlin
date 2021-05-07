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
    compile(kotlinStdlib())
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    shadows(project(":plugins:uast-kotlin-base"))

    // BEWARE: UAST should not depend on IJ platform so that it can work in Android Lint CLI mode (where IDE is not available)
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    testCompileOnly(intellijDep())

    if (Platform.P191.orLower()) {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    compileOnly(intellijDep()) { includeJars("platform-impl") }
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    testRuntime(intellijPluginDep("java"))

    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":idea:idea-test-framework"))

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }

    testRuntimeOnly(toolsJar())
    testRuntime(project(":native:frontend.native"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":idea:idea-gradle"))
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(project(":plugins:parcelize:parcelize-ide"))
    testRuntime(project(":plugins:lombok:lombok-ide-plugin"))
    testRuntime(intellijDep())
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("properties"))
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

testsJar {}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
