
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val scriptingTestDefinition by configurations.creating

dependencies {
    testApi(project(":kotlin-scripting-jvm"))
    testApi(project(":kotlin-scripting-compiler-impl"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:fir:tree"))

    testApi(platform(libs.junit.bom))
    testCompileOnly(project(":compiler:plugin-api"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    scriptingTestDefinition(projectTests(":plugins:scripting:test-script-definition"))
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        generatedTestDir()
    }
}


// Create a Gradle Task for the K2 example repl we can run from an IntelliJ Run Configuration
tasks.register<JavaExec>("runK2ExampleRepl") {
    val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
    group = "application"
    workingDir = rootDir
    description = "Runs the K2 Example Repl"
    mainClass.set("org.jetbrains.kotlin.scripting.test.repl.example.ExampleReplKt")
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    systemProperties["kotlin.script.test.script.definition.classpath"] = scriptingTestDefinitionClasspath
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist", ":plugins:scripting:test-script-definition:testJar")
    workingDir = rootDir
    useJUnitPlatform()
    val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
    doFirst {
        systemProperty("kotlin.script.test.script.definition.classpath", scriptingTestDefinitionClasspath)
    }
}

testsJar()
