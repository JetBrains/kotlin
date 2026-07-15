
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":examples:scripting-jvm-maven-deps"))
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    api(kotlinStdlib())
    compileOnly(project(":compiler:util"))
    compileOnly(project(":kotlin-scripting-compiler"))

    testRuntimeOnly(project(":kotlin-compiler-embeddable"))
    testRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.test {
    useJUnitPlatform()
}
