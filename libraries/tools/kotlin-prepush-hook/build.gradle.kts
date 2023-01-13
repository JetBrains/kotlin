import org.gradle.api.Project
import java.io.File

project.removePrePushHookIfExists()

fun Project.removePrePushHookIfExists() {
    val prePushHookPath = rootProject.getGitDirectory().toPath()
        .resolve("hooks")
        .resolve("pre-push")
    java.nio.file.Files.deleteIfExists(prePushHookPath)
}

fun Project.getGitDirectory(): File {
    val dotGitFile = File(projectDir, ".git")

    return if (dotGitFile.isFile) {
        val workTreeLink = dotGitFile.readLines().single { it.startsWith("gitdir: ") }
        val mainRepoPath = workTreeLink
            .substringAfter("gitdir: ", "")
            .substringBefore("/.git/worktrees/", "")
            .also { require(it.isNotEmpty()) }

        File(mainRepoPath, ".git").also { require(it.isDirectory) }
    } else {
        dotGitFile
    }
}