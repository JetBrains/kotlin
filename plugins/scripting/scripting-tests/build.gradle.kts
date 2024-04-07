
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

    testApi(platform(libs.junit.bom))
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
