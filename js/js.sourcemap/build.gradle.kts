plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    java
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":js:js.ast"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)

    testImplementation(kotlinTest())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask()
}
