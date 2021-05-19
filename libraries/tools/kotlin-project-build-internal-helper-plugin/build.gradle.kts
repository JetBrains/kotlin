
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    java
    kotlin("jvm")
    `java-gradle-plugin`
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

fun DependencyHandler.compileEmbedded(dependencyNotation: String, fn: ExternalModuleDependency.() -> Unit) {
    embedded(dependencyNotation) { fn() }
    compileOnly(dependencyNotation) { fn() }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlinStdlib())
    compileEmbedded(intellijDep()) {
        isTransitive = false
        includeJars( "javac2", "asm-all", rootProject = rootProject)
    }
    testImplementation("junit:junit:4.12")
}

tasks.named("jar") {
    enabled = false
}
val shadowJar = tasks.register<ShadowJar>("shadowJar") {
    from(mainSourceSet.output)
}

publish()
sourcesJar()
javadocJar()
runtimeJar(shadowJar)
