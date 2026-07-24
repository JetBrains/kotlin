import org.gradle.api.Project
import org.gradle.api.Task
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

    fun <T: Task> T.configureRetry(attempts: Int, taskAction: T.() -> Unit) {
        doFirst {
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
