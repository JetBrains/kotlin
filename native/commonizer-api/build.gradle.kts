plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
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
    testImplementation(projectTests(":compiler:tests-common"))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-gradle-plugin-model"))
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(gradleKotlinDsl())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = false) {
    workingDir = projectDir
}

runtimeJar()
sourcesJar()
javadocJar()
