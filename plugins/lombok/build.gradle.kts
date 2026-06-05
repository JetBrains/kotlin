description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val guavaForTests = configurations.dependencyScope("guavaForTests").get()
val guavaClasspathForTests = configurations.resolvable("guavaClasspathForTests") {
    extendsFrom(guavaForTests)
    isTransitive = false
}.get()

val slf4jApiForTests = configurations.dependencyScope("slf4jApiForTests").get()

val slf4jApiClasspathForTests = configurations.resolvable("slf4jApiClasspathForTests") {
    extendsFrom(slf4jApiForTests)
    isTransitive = false
}.get()

val slf4jExtForTests = configurations.dependencyScope("slf4jExtForTests").get()

val slf4jExtClasspathForTests = configurations.resolvable("slf4jExtClasspathForTests") {
    extendsFrom(slf4jExtForTests)
    isTransitive = false
}.get()

val log4jOverSlf4jForTests = configurations.dependencyScope("log4jOverSlf4jForTests").get()

val log4jOverSlf4jClasspathForTests = configurations.resolvable("log4jOverSlf4jClasspathForTests") {
    extendsFrom(log4jOverSlf4jForTests)
    isTransitive = false
}.get()

val commonsLoggingForTests = configurations.dependencyScope("commonsLoggingForTests").get()

val commonsLoggingClasspathForTests = configurations.resolvable("commonsLoggingClasspathForTests") {
    extendsFrom(commonsLoggingForTests)
    isTransitive = false
}.get()

val floggerForTests = configurations.dependencyScope("floggerForTests").get()

val floggerClasspathForTests = configurations.resolvable("floggerClasspathForTests") {
    extendsFrom(floggerForTests)
    isTransitive = false
}.get()

val floggerSystemBackendForTests = configurations.dependencyScope("floggerSystemBackendForTests").get()

val floggerSystemBackendClasspathForTests = configurations.resolvable("floggerSystemBackendClasspathForTests") {
    extendsFrom(floggerSystemBackendForTests)
    isTransitive = false
}.get()

val jbossLoggingForTests = configurations.dependencyScope("jbossLoggingForTests").get()
val jbossLoggingClasspathForTests = configurations.resolvable("jbossLoggingClasspathForTests") {
    extendsFrom(jbossLoggingForTests)
    isTransitive = false
}.get()

val log4j2ApiForTests = configurations.dependencyScope("log4j2ApiForTests").get()

val log4j2ApiClasspathForTests = configurations.resolvable("log4j2ApiClasspathForTests") {
    extendsFrom(log4j2ApiForTests)
    isTransitive = false
}.get()

val log4j2CoreForTests = configurations.dependencyScope("log4j2CoreForTests").get()

val log4j2CoreClasspathForTests = configurations.resolvable("log4j2CoreClasspathForTests") {
    extendsFrom(log4j2CoreForTests)
    isTransitive = false
}.get()

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(intellijCore())
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.common"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k1"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k2"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.cli"))

    testFixturesApi(commonDependency("org.projectlombok:lombok"))

    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(libs.junit4)

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())

    guavaForTests(libs.guava)
    slf4jApiForTests(libs.slf4j.api)
    slf4jExtForTests(libs.slf4j.ext)
    log4jOverSlf4jForTests(libs.log4j.over.slf4j)
    commonsLoggingForTests(libs.commons.logging)
    floggerForTests(libs.flogger)
    floggerSystemBackendForTests(libs.flogger.system.backend)
    jbossLoggingForTests(libs.jboss.logging)
    log4j2ApiForTests(libs.log4j2.api)
    log4j2CoreForTests(libs.log4j2.core)
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0)
    ) {
        testInputsCheck {
            // Log4j2's `LogManager.getLogger(...)` calls `System.getProperties()` during initialization,
            // which requires read+write access on `java.util.PropertyPermission "*"`.
            with(extraPermissions) {
                add("permission java.util.PropertyPermission \"*\", \"read,write\";")
            }
        }

        val prefix = "org.jetbrains.kotlin.test"
        addClasspathProperty(guavaClasspathForTests, "$prefix.guava")
        addClasspathProperty(slf4jApiClasspathForTests, "$prefix.slf4j-api")
        addClasspathProperty(slf4jExtClasspathForTests, "$prefix.slf4j-ext")
        addClasspathProperty(log4jOverSlf4jClasspathForTests, "$prefix.log4j-over-slf4j")
        addClasspathProperty(commonsLoggingClasspathForTests, "$prefix.commons-logging")
        addClasspathProperty(floggerClasspathForTests, "$prefix.flogger")
        addClasspathProperty(floggerSystemBackendClasspathForTests, "$prefix.flogger-system-backend")
        addClasspathProperty(jbossLoggingClasspathForTests, "$prefix.jboss-logging")
        addClasspathProperty(log4j2ApiClasspathForTests, "$prefix.log4j-api")
        addClasspathProperty(log4j2CoreClasspathForTests, "$prefix.log4j-core")
    }

    testGenerator("org.jetbrains.kotlin.lombok.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withTestJar()

    testData(project(":kotlin-lombok-compiler-plugin").isolated, "testData")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
