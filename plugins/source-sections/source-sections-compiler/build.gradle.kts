
description = "Kotlin SourceSections Compiler Plugin"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.script"))
    compileOnly(project(":compiler:plugin-api"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.script"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":compiler.tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":kotlin-daemon-client"))
}

sourceSets {
    "main" { default() }
    "test" { default() }
}

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("KOTLIN_HOME", rootProject.extra["distKotlinHomeDir"])
    ignoreFailures = true
}

runtimeJar()
sourcesJar()
javadocJar()

dist()

publish()
