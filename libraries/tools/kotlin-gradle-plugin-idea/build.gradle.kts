import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME

plugins {
    kotlin("jvm")
    `java-test-fixtures`
    `maven-publish`
}

kotlin.sourceSets.configureEach {
    languageSettings.apiVersion = "1.4"
    languageSettings.languageVersion = "1.4"
    languageSettings.optIn("org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi")
}

dependencies {
    api(project(":kotlin-tooling-core"))
    implementation(kotlinStdlib())
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-statistics"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))

    testImplementation("org.reflections:reflections:0.10.2") {
        because("Tests on the object graph are performed. This library will find implementations of interfaces at runtime")
    }

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleKotlinDsl())
    testFixturesImplementation(project(":kotlin-tooling-core"))
    testFixturesImplementation(project(":kotlin-test:kotlin-test-junit"))
}

publish(moduleMetadata = true) {
    val kotlinLibraryComponent = components[ADHOC_COMPONENT_NAME] as AdhocComponentWithVariants
    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { mapToMavenScope("compile") }
    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { mapToMavenScope("runtime") }
    suppressAllPomMetadataWarnings()
}

javadocJar()
sourcesJar()

//region Setup: Backwards compatibility tests

run {
    val compatibilityTestClasspath by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }

    dependencies {
        compatibilityTestClasspath(project(":kotlin-gradle-plugin-idea-for-compatibility-tests"))
    }

    tasks.test {
        dependsOnKotlinGradlePluginInstall()
        dependsOn(compatibilityTestClasspath)
        val conf: FileCollection = compatibilityTestClasspath
        inputs.files(conf)
        doFirst { systemProperty("compatibilityTestClasspath", conf.files.joinToString(";") { it.absolutePath }) }
    }
}

//endregion
