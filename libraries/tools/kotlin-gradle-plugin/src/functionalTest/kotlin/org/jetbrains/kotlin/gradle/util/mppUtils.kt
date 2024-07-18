import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.toJson
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask

/**
 * During normal build the task `generateProjectStructureMetadata` generates kotlin-project-structure-metadata.json.
 * But with functional tests, we don't have a Gradle execution phase, so we need to manually put psm data into the task output.
 */
internal fun mockProjectStructureMetadataFileForProject(b: ProjectInternal) {
    b.locateOrRegisterGenerateProjectStructureMetadataTask().get()
        .resultFile.also { it.parentFile.mkdirs() }.writeText(b.multiplatformExtension.kotlinProjectStructureMetadata.toJson())
}