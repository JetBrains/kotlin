plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val JDK_16: String by project
jvmTarget = "1.6"
javaHome = JDK_16

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-Xsuppress-deprecated-jvm-target-warning"
    )
}

tasks.withType<Test> {
    executable = "$JDK_16/bin/java"
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
