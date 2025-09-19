plugins {
    kotlin("jvm")
    id("gradle-plugin-published-compiler-dependency-configuration")
    id("project-tests-convention")
}

kotlin {
    explicitApi()
}

description = "Kotlin KLIB Library Commonizer API"
publish()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":native:kotlin-native-utils"))
    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(gradleKotlinDsl())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4) {
        workingDir = projectDir
    }
}

runtimeJar()
sourcesJar()
javadocJar()
