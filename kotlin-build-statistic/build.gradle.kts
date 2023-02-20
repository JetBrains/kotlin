import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as GradleKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

description = "Kotlin Build Report Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":kotlin-util-io"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    implementation(commonDependency("com.google.code.gson:gson"))
    testApi(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest(parallel = true)

projectTest("testJUnit5", jUnitMode = JUnitMode.JUnit5, parallel = true) {
    useJUnitPlatform()
}

// 1.9 level breaks Kotlin Gradle plugins via changes in enums (KT-48872)
// We limit api and LV until KGP will stop using Kotlin compiler directly (KT-56574)
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}
