description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val guavaForTests by configurations.dependencyScope("guavaForTests")
val guavaClasspathForTests by configurations.resolvable("guavaClasspathForTests") {
    extendsFrom(guavaForTests)
    isTransitive = false
}
val slf4jApiForTests by configurations.dependencyScope("slf4jApiForTests")
val slf4jApiClasspathForTests by configurations.resolvable("slf4jApiClasspathForTests") {
    extendsFrom(slf4jApiForTests)
    isTransitive = false
}
val slf4jExtForTests by configurations.dependencyScope("slf4jExtForTests")
val slf4jExtClasspathForTests by configurations.resolvable("slf4jExtClasspathForTests") {
    extendsFrom(slf4jExtForTests)
    isTransitive = false
}
val log4jOverSlf4jForTests by configurations.dependencyScope("log4jOverSlf4jForTests")
val log4jOverSlf4jClasspathForTests by configurations.resolvable("log4jOverSlf4jClasspathForTests") {
    extendsFrom(log4jOverSlf4jForTests)
    isTransitive = false
}
val commonsLoggingForTests by configurations.dependencyScope("commonsLoggingForTests")
val commonsLoggingClasspathForTests by configurations.resolvable("commonsLoggingClasspathForTests") {
    extendsFrom(commonsLoggingForTests)
    isTransitive = false
}
val floggerForTests by configurations.dependencyScope("floggerForTests")
val floggerClasspathForTests by configurations.resolvable("floggerClasspathForTests") {
    extendsFrom(floggerForTests)
    isTransitive = false
}
val floggerSystemBackendForTests by configurations.dependencyScope("floggerSystemBackendForTests")
val floggerSystemBackendClasspathForTests by configurations.resolvable("floggerSystemBackendClasspathForTests") {
    extendsFrom(floggerSystemBackendForTests)
    isTransitive = false
}
val jbossLoggingForTests by configurations.dependencyScope("jbossLoggingForTests")
val jbossLoggingClasspathForTests by configurations.resolvable("jbossLoggingClasspathForTests") {
    extendsFrom(jbossLoggingForTests)
    isTransitive = false
}
val log4j2ApiForTests by configurations.dependencyScope("log4j2ApiForTests")
val log4j2ApiClasspathForTests by configurations.resolvable("log4j2ApiClasspathForTests") {
    extendsFrom(log4j2ApiForTests)
    isTransitive = false
}
val log4j2CoreForTests by configurations.dependencyScope("log4j2CoreForTests")
val log4j2CoreClasspathForTests by configurations.resolvable("log4j2CoreClasspathForTests") {
    extendsFrom(log4j2CoreForTests)
    isTransitive = false
}

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
