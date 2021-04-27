plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:light-classes"))
    implementation(project(":plugins:uast-kotlin-base"))

    // BEWARE: Uast should not depend on IDEA.
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }
    compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }

    implementation(project(":idea:idea-frontend-independent"))
    implementation(project(":idea:idea-frontend-api"))
    implementation(project(":idea:idea-frontend-fir"))

    testImplementation(toolsJar())
    testImplementation(commonDep("junit:junit"))
    testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":idea:idea-test-framework"))
    testImplementation(projectTests(":idea"))
    testImplementation(projectTests(":idea:idea-fir"))
    // To compare various aspects (e.g., render, log, type, value, etc.) against legacy UAST Kotlin
    testImplementation(projectTests(":plugins:uast-kotlin"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
    val useFirIdeaPlugin = kotlinBuildProperties.useFirIdeaPlugin
    doFirst {
        if (!useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}

testsJar ()
