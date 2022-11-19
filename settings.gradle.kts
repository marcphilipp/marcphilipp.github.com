import org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "marcphilipp.github.io"

include("speakerdeck-api")
include("talks-postprocessor")
