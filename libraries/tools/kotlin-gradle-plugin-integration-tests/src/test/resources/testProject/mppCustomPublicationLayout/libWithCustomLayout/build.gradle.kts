plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    linuxX64()
    linuxArm64()
}

// Gradle magical spell to access SoftwareComponentFactory
abstract class SoftwareComponentFactoryProvider @Inject constructor(
    val softwareComponentFactory: SoftwareComponentFactory
)
val softwareComponentFactoryProvider = project.objects.newInstance<SoftwareComponentFactoryProvider>()
val customComponent = softwareComponentFactoryProvider.softwareComponentFactory.adhoc("customKotlin")

afterEvaluate {
    kotlin.targets.all {
        val configuration = configurations.getByName(apiElementsConfigurationName)
        configuration.artifacts.forEach {
            it as ConfigurablePublishArtifact
            it.classifier = targetName
        }
        customComponent.addVariantsFromConfiguration(configuration) {
            // workaround for the issue that secondary variant are actually published: https://github.com/gradle/gradle/issues/29295
            // unless something specific is done
            if (configurationVariant.name != configuration.name) {
                skip()
            }
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("kotlin") {
            from(customComponent)
        }
    }
}