pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "augmego-minecraft"

include(":shared:avatar-core")
include(":minecraft-mod")
