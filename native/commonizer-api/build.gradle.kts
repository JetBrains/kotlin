import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
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
    workingDir = projectDir
}

runtimeJar()
sourcesJar()
javadocJar()

// 1.9 level breaks Kotlin Gradle plugins via changes in enums (KT-48872)
// We limit api and LV until KGP will stop using Kotlin compiler directly (KT-56574)
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}
