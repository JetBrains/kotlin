import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") apply false
    `maven-publish`
}

val disambiguationAttribute = Attribute.of("disambiguationAttribute", String::class.java)


fun testResolutionToSourcesVariant(
    targetName: String,
    platformType: KotlinPlatformType,
    nativePlatform: String? = null,
    includeDisambiguation: Boolean = true
) {
    val configuration = configurations.create("${targetName}Sources") {
        isCanBeResolved = true
        isCanBeConsumed = false

        if (platformType == KotlinPlatformType.jvm) {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-runtime"))
        } else {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
        }
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))

        attributes.attribute(KotlinPlatformType.attribute, platformType)
        if (nativePlatform != null) {
            attributes.attribute(KotlinNativeTarget.konanTargetAttribute, nativePlatform)
        }

        if (includeDisambiguation) {
            attributes.attribute(disambiguationAttribute, targetName)
        }

        project.dependencies.add(name, "test:lib:1.0")
    }

    val files = configuration.resolve()
    println("<RESOLVED SOURCES FILE $targetName>")
    println(files.joinToString("\n") { it.name })
    println("</RESOLVED SOURCES FILE $targetName>")

    println("<RESOLVED DEPENDENCIES OF $targetName>")
    val resolvedVariants = configuration
        .incoming
        .resolutionResult
        .allDependencies
        .filterIsInstance<ResolvedDependencyResult>()
        .map { it.requested.displayName to it.resolvedVariant.displayName }
        .joinToString("\n") { "${it.first} => ${it.second}" }
    println(resolvedVariants)
    println("</RESOLVED DEPENDENCIES OF $targetName>")
}