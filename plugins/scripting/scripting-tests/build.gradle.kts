
plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

val scriptingTestDefinition by configurations.creating

dependencies {
    testFixturesApi(project(":kotlin-scripting-jvm"))
    testFixturesApi(project(":kotlin-scripting-compiler-impl"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:fir:tree"))

    testFixturesApi(platform(libs.junit.bom))
    testCompileOnly(project(":compiler:plugin-api"))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    scriptingTestDefinition(testFixtures(project(":plugins:scripting:test-script-definition")))
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
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

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist", ":plugins:scripting:test-script-definition:testJar")
        workingDir = rootDir
        val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
        doFirst {
            systemProperty("kotlin.script.test.script.definition.classpath", scriptingTestDefinitionClasspath)
        }
    }
}

testsJar()
