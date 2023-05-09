plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

kotlin {
    targetHierarchy.default()

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
        customComponent.addVariantsFromConfiguration(configuration) {}
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