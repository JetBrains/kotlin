import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
}

dependencies {
    compile(gradleApi())
    compile(project(":kotlin-gradle-plugin-api"))
    compile(project(":kotlin-native:kotlin-native-utils"))
}

val generateMppTargetContainerWithPresets by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppPresetFunctionsCodegenKt",
    sourceSets["main"]
)

val generateAbstractBinaryContainer by generator(
    "org.jetbrains.kotlin.generators.gradle.dsl.MppNativeBinaryDSLCodegenKt",
    sourceSets["main"]
)

listOf(generateMppTargetContainerWithPresets, generateAbstractBinaryContainer).forEach {
    it.systemProperty(
        "org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/main/kotlin").absolutePath
    )
}

// Workaround: 'java -jar' refuses to read the original dotted filename on Windows, 'Unable to access jarFile org.jetbrains.kotlin....jar'
tasks.named<Jar>(generateMppTargetContainerWithPresets.name + "WriteClassPath").configure {
    archiveName = generateMppTargetContainerWithPresets.name
}
