description = "Lightweight annotation processing support – Kotlin compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))

    compileOnly(project(":kapt-lite:kapt-lite-kdoc"))
    embedded(project(":kapt-lite:kapt-lite-kdoc")) { isTransitive = false }

    compileOnly(project(":kapt-lite:kapt-lite-signature-parser"))
    embedded(project(":kapt-lite:kapt-lite-signature-parser")) { isTransitive = false }

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testCompile(toolsJar())
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

testsJar {}

projectTest {
    workingDir = rootDir
}