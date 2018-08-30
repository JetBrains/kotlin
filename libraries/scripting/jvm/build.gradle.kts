import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-script-util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin.experimental.coroutines = Coroutines.ENABLE

val jar = runtimeJar()
val sourcesJar = sourcesJar()
val javadocJar = javadocJar()

dist()

ideaPlugin {
    from(jar, sourcesJar)
}

standardPublicJars()

publish()
