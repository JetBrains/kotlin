import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

open class MyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.multiplatform")

        target.extensions.getByType<KotlinMultiplatformExtension>(
            KotlinMultiplatformExtension::class.java
        ).apply {
            js { browser { } }
        }
    }
}