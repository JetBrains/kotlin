import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

gradle.afterProject {
    plugins.withType<KotlinBasePlugin> {
        extensions.configure<KotlinTopLevelExtension> {
            // Should be in sync with 'gradle-settings-conventions/gradle.properties'
            kotlinDaemonJvmArgs = listOf(
                "-Xmx3g",
                "-Dkotlin.js.compiler.legacy.force_enabled=true"
            )
        }
    }
}
