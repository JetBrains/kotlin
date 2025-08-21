import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

abstract class CheckPlatformDependenciesTestTask : DefaultTask() {
    @get:InputFiles
    abstract val dependencies: ConfigurableFileCollection

    @get:Input
    abstract val sourceSetName: Property<String>

    fun setSourceSet(sourceSet: DefaultKotlinSourceSet) {
        // Dependencies forwarded to the IDE will be attached to the intransitiveMetadataConfiguration
        dependencies.setFrom(project.configurations.getByName(sourceSet.intransitiveMetadataConfigurationName))
        sourceSetName.set(sourceSet.name)
    }

    @TaskAction
    fun action() {
        val sourceSetName = sourceSetName.get()
        dependencies.forEach { dependency ->
            logger.quiet("$sourceSetName dependency: ${dependency.path}")
        }

        dependencies.forEach { dependency ->
            if ("klib${File.separator}commonized" in dependency.path) {
                throw AssertionError(
                    "$sourceSetName: Found unexpected commonized dependency ${dependency.path}"
                )
            }
        }

        if (dependencies.isEmpty) {
            throw AssertionError("$sourceSetName: Expected at least one platform dependency")
        }

        val platformKlibPattern = "klib${File.separator}platform"
        if (dependencies.none { dependency -> platformKlibPattern in dependency.path }) {
            throw AssertionError("$sourceSetName: Expected at least one dependency from '$platformKlibPattern'")
        }
    }
}

fun createPlatformDependenciesTestTask(sourceSet: DefaultKotlinSourceSet) {
    val taskName = "check${sourceSet.name.capitalize()}PlatformDependencies"
    val task = tasks.register<CheckPlatformDependenciesTestTask>(taskName) {
        setSourceSet(sourceSet)
        dependsOn("commonize")
    }
    tasks.maybeCreate("checkPlatformDependencies").dependsOn(task)
}

kotlin {
    linuxX64()
    linuxArm64()

    createPlatformDependenciesTestTask(sourceSets.getByName("linuxX64Main") as DefaultKotlinSourceSet)
    createPlatformDependenciesTestTask(sourceSets.getByName("linuxArm64Main") as DefaultKotlinSourceSet)
}
