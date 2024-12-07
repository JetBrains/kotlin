import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import java.net.URI

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()

        ivy {
            name = "Distributions at https://nodejs.org/dist"
            url = URI("https://nodejs.org/dist")

            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
    }
}
