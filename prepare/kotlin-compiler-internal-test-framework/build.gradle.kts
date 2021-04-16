import org.gradle.jvm.tasks.Jar

plugins {
    java
    `maven-publish`
}

val embedded by configurations

dependencies {
    embedded(project(":compiler:serialization")){ isTransitive = false }
    embedded(projectTests(":compiler:tests-common-jvm6")){ isTransitive = false }
    embedded(project(":kotlin-scripting-compiler-impl")){ isTransitive = false }
    embedded(projectTests(":compiler:test-infrastructure-utils")){ isTransitive = false }
    embedded(projectTests(":compiler:tests-compiler-utils")){ isTransitive = false }
    embedded(projectTests(":compiler:tests-common")) { isTransitive = false }
}

publish()
noDefaultJar()

val runtimeJar: TaskProvider<out Jar> = runtimeJar(unshadedCompiler())
val runtimeConfig by configurations.creating
artifacts {
    val jar: Jar = runtimeJar.get()
    add(runtimeConfig.name, jar)
}
publishing {
    publications {
        create<MavenPublication>("maven"){
            artifact(runtimeJar)
        }
    }
}
sourcesJar()
javadocJar()

val testCompilationClasspath by configurations.creating
val testCompilerClasspath by configurations.creating {
    isCanBeConsumed = false
    extendsFrom(configurations["runtimeElements"])
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

projectTest {
    dependsOn(runtimeJar)
    doFirst {
        systemProperty("compilerClasspath", "${runtimeJar.get().outputs.files.asPath}${File.pathSeparator}${testCompilerClasspath.asPath}")
        systemProperty("compilationClasspath", testCompilationClasspath.asPath)
    }
}