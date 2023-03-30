import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat.FRAMEWORK
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform") apply false
}

data class ResolutionRequest(
    val name: String? = null,
    val buildType: NativeBuildType,
    val targets: List<KonanTarget>
) {
    constructor(buildType: NativeBuildType, vararg targets: KonanTarget) : this(null, buildType, targets.toList())
    constructor(name: String, buildType: NativeBuildType, vararg targets: KonanTarget) : this(name, buildType, targets.toList())
}

/**
 * Create a configuration with given input and try to resolve a variant from :producer project
 */
fun Project.resolve(request: ResolutionRequest): String? {
    val configuration = configurations.detachedConfiguration(dependencies.create(project(":producer")))

    configuration.apply {
        isCanBeResolved = true
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attribute(ArtifactAttributes.ARTIFACT_FORMAT, FRAMEWORK)
            attribute(KotlinNativeTarget.kotlinNativeBuildTypeAttribute, request.buildType.name)
            attribute(Framework.frameworkTargets, request.targets.map { it.name }.toSet())
            if (request.name != null) {
                attribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute, request.name)
            }
        }
    }

    return configuration.incoming
        .resolutionResult
        .allDependencies
        .filterIsInstance<ResolvedDependencyResult>()
        .mapNotNull { (it.selected.id as? ProjectComponentIdentifier)?.to(it.resolvedVariant) }
        .firstOrNull { (id, variant) -> id.projectName == "producer" }
        ?.second
        ?.displayName
}

infix fun ResolutionRequest.shouldResolveTo(variantName: String) {
    val actualVariant = resolve(this)
    if (variantName == actualVariant) {
        println("i: RESOLUTION_SUCCESS:$variantName")
    } else {
        println("e: RESOLUTION_FAILURE:$this resolved to '$actualVariant' but expected '$variantName'")
    }
}

ResolutionRequest("dynamic", DEBUG, IOS_ARM64) shouldResolveTo "dynamicDebugFrameworkIosArm64"
ResolutionRequest("dynamic", DEBUG, IOS_X64) shouldResolveTo "dynamicDebugFrameworkIosX64"
ResolutionRequest(DEBUG, IOS_ARM64, IOS_X64) shouldResolveTo "dynamicDebugFrameworkIosFat"

ResolutionRequest("dynamic", DEBUG, MACOS_ARM64) shouldResolveTo "dynamicDebugFrameworkMacosArm64"
ResolutionRequest("dynamic", DEBUG, MACOS_X64) shouldResolveTo "dynamicDebugFrameworkMacosX64"
ResolutionRequest(DEBUG, MACOS_ARM64, MACOS_X64) shouldResolveTo "dynamicDebugFrameworkOsxFat"

ResolutionRequest("macosArm64Specific", DEBUG, MACOS_ARM64) shouldResolveTo "macosArm64SpecificDebugFrameworkMacosArm64"
ResolutionRequest("macosArm64Specific", RELEASE, MACOS_ARM64) shouldResolveTo "macosArm64SpecificReleaseFrameworkMacosArm64"

ResolutionRequest("macosX64Specific", DEBUG, MACOS_X64) shouldResolveTo "macosX64SpecificDebugFrameworkMacosX64"
ResolutionRequest("macosX64Specific", RELEASE, MACOS_X64) shouldResolveTo "macosX64SpecificReleaseFrameworkMacosX64"

ResolutionRequest("static", RELEASE, IOS_ARM64) shouldResolveTo "staticReleaseFrameworkIosArm64"
ResolutionRequest("static", RELEASE, IOS_X64) shouldResolveTo "staticReleaseFrameworkIosX64"
ResolutionRequest(RELEASE, IOS_X64, IOS_ARM64) shouldResolveTo "staticReleaseFrameworkIosFat"

ResolutionRequest("static", RELEASE, MACOS_ARM64) shouldResolveTo "staticReleaseFrameworkMacosArm64"
ResolutionRequest("static", RELEASE, MACOS_X64) shouldResolveTo "staticReleaseFrameworkMacosX64"
ResolutionRequest(RELEASE, MACOS_ARM64, MACOS_X64) shouldResolveTo "staticReleaseFrameworkOsxFat"