import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.withType

fun Project.configurePublishingRetry() {
    val publishingAttempts = findProperty("kotlin.build.publishing.attempts")?.toString()?.toInt()

    fun retry(attempts: Int, action: () -> Unit): Boolean {
        repeat(attempts) {
            try {
                action()
                return true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun <T : PublishToMavenRepository> T.configureRetry(attempts: Int, taskAction: T.() -> Unit) {
        // Only remote publishing can fail transiently, so skip retrying local `file:` repositories.
        // Resolved lazily because the repository is set after the task is created.
        val isLocalRepository = project.providers.provider { repository.url.scheme == "file" }
        doFirst {
            if (isLocalRepository.get()) return@doFirst

            if (retry(attempts) { taskAction() })
                throw StopExecutionException()
            else
                error("Number of attempts ($attempts) exceeded for ${project.path}:$name")
        }
    }

    if (publishingAttempts != null && publishingAttempts > 1) {
        tasks.withType<PublishToMavenRepository> {
            configureRetry(publishingAttempts, PublishToMavenRepository::publish)
        }
    }
}
