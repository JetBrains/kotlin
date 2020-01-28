
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))

    // BEWARE: Uast should not depend on IDEA.
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    testCompileOnly(intellijDep())

    if (Platform.P191.orLower()) {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    if (Platform.P192.orHigher()) {
        compileOnly(intellijDep()) { includeJars("platform-impl") }
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testRuntime(intellijPluginDep("java"))
    }

    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":idea:idea-test-framework"))

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }

    testRuntime(project(":native:frontend.native")) { isTransitive = false }
    testRuntime(project(":native:kotlin-native-utils")) { isTransitive = false }
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

testsJar {}

projectTest(parallel = true) {
    workingDir = rootDir
}
