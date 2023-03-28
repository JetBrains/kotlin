import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon

gradle.afterProject {
    plugins.withType<KotlinBasePlugin> {
        tasks.withType<CompileUsingKotlinDaemon>().configureEach {
            // Should be in sync with 'gradle-settings-conventions/gradle.properties'
            kotlinDaemonJvmArguments.set(listOf("-Xmx3g"))
        }
    }
}
