import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import java.util.concurrent.Callable

/**
 * From repoDependencies.kt
 */

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

fun DependencyHandler.add(configurationName: String, dependencyNotation: Any, configure: (ModuleDependency.() -> Unit)?) {
    // Avoid `dependencyNotation` to `ModuleDependency` class cast exception if possible
    if (configure != null) {
        add(configurationName, dependencyNotation, closureOf(configure))
    } else {
        add(configurationName, dependencyNotation)
    }
}

fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    body: ConfigurablePublishArtifact.() -> Unit = {}
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, task, body)
}

/**
 * From CommonUtils.kt
 */
inline fun CopySourceSpec.from(crossinline filesProvider: () -> Any?): CopySourceSpec = from(Callable { filesProvider() })

fun Project.javaPluginExtension(): JavaPluginExtension = extensions.getByType()

fun Project.findJavaPluginExtension(): JavaPluginExtension? = extensions.findByType()