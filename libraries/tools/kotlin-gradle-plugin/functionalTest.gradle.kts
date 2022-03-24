import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val mainSourceSet: SourceSet = sourceSets["main"]
val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest") {
    compileClasspath += mainSourceSet.output
    runtimeClasspath += mainSourceSet.output

    configurations.getByName(implementationConfigurationName) {
        extendsFrom(configurations.getByName(mainSourceSet.implementationConfigurationName))
        extendsFrom(configurations.getByName("testImplementation"))
    }

    configurations.getByName(runtimeOnlyConfigurationName) {
        extendsFrom(configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName))
        extendsFrom(configurations.getByName("testRuntimeOnly"))
    }
}

project.extensions.getByType<KotlinJvmProjectExtension>().target.compilations {
    named(functionalTestSourceSet.name) {
        associateWith(this@compilations.getByName("main"))
        associateWith(this@compilations.getByName("common"))
    }
}

val functionalTest by tasks.register<Test>("functionalTest") {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs functional tests"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    workingDir = projectDir
    dependsOnKotlinGradlePluginInstall()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named("check") {
    dependsOn(functionalTest)
}
