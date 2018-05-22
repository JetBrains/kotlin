import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Scripting Compiler Plugin for Gradle"

plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    `java-gradle-plugin`
}

val packedJars by configurations.creating

dependencies {
    compile(project(":kotlin-gradle-plugin-api"))
    compileOnly(project(":kotlin-scripting-compiler"))
    packedJars(project(":kotlin-scripting-compiler")) { isTransitive = false }
    runtime(project(":kotlin-scripting-common"))
    runtime(project(":kotlin-scripting-jvm"))
    runtime(project(":kotlin-scripting-misc"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar: Jar by tasks
jar.apply {
    classifier = "base"
}

runtimeJar(rewriteDepsToShadedCompiler(
    task<ShadowJar>("shadowJar")  {
        from(packedJars)
        from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    }
))
sourcesJar()
javadocJar()

val gradlePluginId = "org.jetbrains.kotlin.plugin.scripting"

pluginBundle {
    (plugins) {
        gradlePluginId {
            id = gradlePluginId
            displayName = "Gradle plugin for kotlin scripting"
            description = displayName
        }
    }
}

publish()

projectTest {
    workingDir = rootDir
}
