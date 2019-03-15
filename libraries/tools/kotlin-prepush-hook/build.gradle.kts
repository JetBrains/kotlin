import org.gradle.api.Project
import java.io.File

apply { plugin("java") }
apply { plugin("jps-compatible") }

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

project.addPrePushHookIfMissing()

fun Project.addPrePushHookIfMissing() {
    val dotGitDirectory = rootProject.getGitDirectory()
    val hooksDirectory = File(dotGitDirectory, "hooks").also { it.mkdirs() }

    val prePushHook = File(projectDir, "pre-push.sh").also { require(it.exists()) }
    val prePushTarget = File(hooksDirectory, "pre-push")
    prePushHook.copyTo(prePushTarget, overwrite = true)
    prePushTarget.setExecutable(true, true)
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