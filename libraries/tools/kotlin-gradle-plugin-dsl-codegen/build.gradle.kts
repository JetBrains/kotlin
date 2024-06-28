plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(gradleApi())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":native:kotlin-native-utils"))
}

val generateGroupName = "Generate"

val generateMppTargetContainerWithPresets by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppPresetFunctionsCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateAbstractKotlinArtifactsExtensionImplementation by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KotlinArtifactsDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}

val generateMppSourceSetConventions by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppSourceSetConventionsCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    setKGPSourceRootPaths()
}


fun JavaExec.setKGPSourceRootPaths() {
    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/common/kotlin").absolutePath
    )

    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.kotlinGradlePluginApiSourceRoot",
        project(":kotlin-gradle-plugin-api").projectDir.resolve("src/common/kotlin").absolutePath
    )
}
