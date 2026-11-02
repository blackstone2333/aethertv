pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "AetherCinema"
include(":app")
// Optional same-signature integration probe; never included in the production application.
if (providers.gradleProperty("cinemaProbe").isPresent) {
    include(":probe")
    project(":probe").projectDir = file("tools/probe")
}
