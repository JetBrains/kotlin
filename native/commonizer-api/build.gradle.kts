plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApi()
}

description = "Kotlin KLIB Library Commonizer API"
publish()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":kotlin-test::kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
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
    dependsOn(":dist")
    workingDir = projectDir
}

runtimeJar()
sourcesJar()
javadocJar()
