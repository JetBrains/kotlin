import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":kotlin-contracts-plugin"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("annotations", "trove4j", "guava", rootProject = rootProject) }
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar") {
    dependsOn(":kotlin-contracts-plugin:dist")
}

ideaPlugin {
    from(jar)
}

projectTest {
    dependsOn(subprojects.map { "${it.name}:dist" })
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.contracts.contextual.GenerateContractTestsKt")
