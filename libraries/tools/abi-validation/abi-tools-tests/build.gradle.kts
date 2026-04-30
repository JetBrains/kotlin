import org.gradle.kotlin.dsl.project

plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

val buildToolsApiImpl = configurations.dependencyScope("buildToolsApiImpl")
val buildToolsApiImplResolvable = configurations.resolvable("buildToolsApiImplResolvable") {
    extendsFrom(buildToolsApiImpl.get())
}

val abiToolsImpl = configurations.dependencyScope("abiToolsImpl")
val abiToolsImplResolvable = configurations.resolvable("abiToolsImplResolvable") {
    extendsFrom(abiToolsImpl.get())
}

val ABI_TOOLS_EMBEDDABLE_PROPERTY = "abi.tools.embeddable.classpath"
val BUILD_TOOLS_IMPL_PROPERTY = "build.tools.impl.classpath"

dependencies {
    // common dependencies for all tests
    testImplementation(kotlinStdlib())
    testImplementation(kotlinTest("junit"))

    testImplementation(project(":libraries:tools:abi-validation:abi-tools-api"))
    testImplementation(project(":libraries:tools:abi-validation:abi-tools"))
    testImplementation(project(":compiler:build-tools:kotlin-build-tools-api"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)

    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-impl"))
    abiToolsImpl(project(":libraries:tools:abi-validation:abi-tools"))
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        val initDumps = project.providers.gradleProperty("init.dumps").orNull?.toBoolean() ?: false
        val overwriteDumps = project.providers.gradleProperty("overwrite.output").orNull?.toBoolean() ?: false

        useJUnitPlatform()
        systemProperty("overwrite.output", overwriteDumps)
        systemProperty("init.dumps", initDumps)


        testInputsCheck {
            with(extraPermissions) {
                add("permission java.io.FilePermission \"<no_path>/lib\", \"read\";")
                add("permission java.io.FilePermission \"./kotlin-scripting-compiler.jar\", \"read\";")
                add("permission java.io.FilePermission \"./kotlin-scripting-compiler-impl.jar\", \"read\";")
                add("permission java.io.FilePermission \"./kotlin-scripting-common.jar\", \"read\";")
                add("permission java.io.FilePermission \"./kotlin-scripting-jvm.jar\", \"read\";")

                if (initDumps || overwriteDumps) {
                    add("permission java.io.FilePermission \"src/test/resources/cases/-\", \"read,write\";")
                    add("permission java.io.FilePermission \"src/test/resources/cases-klib/-\", \"read,write\";")
                }
            }

            addClasspathProperty(buildToolsApiImplResolvable.get(), BUILD_TOOLS_IMPL_PROPERTY)
            addClasspathProperty(abiToolsImplResolvable.get(), ABI_TOOLS_EMBEDDABLE_PROPERTY)
        }
    }
}
