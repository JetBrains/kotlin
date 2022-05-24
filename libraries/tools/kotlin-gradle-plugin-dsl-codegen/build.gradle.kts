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
}

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
}

val generateAbstractKotlinArtifactsExtensionImplementation by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KotlinArtifactsDSLCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
    systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/common/kotlin").absolutePath
    )
}

val generateKpmNativeVariants by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.KpmNativeVariantCodegenKt",
    sourceSets["main"]
) {
    group = generateGroupName
}

listOf(generateMppTargetContainerWithPresets, generateAbstractBinaryContainer, generateKpmNativeVariants).forEach {
    it.systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/common/kotlin").absolutePath
    )
}

