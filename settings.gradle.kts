pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement { }

rootProject.name = "Dossier Tau"
include(":app")

includeBuild("../../IdeaProjects/Périscope")

 