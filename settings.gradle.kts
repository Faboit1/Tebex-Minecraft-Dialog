pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "TebexPlugin"

listOf("sdk", "bukkit", "bungeecord", "velocity", "folia", "fabric-26.1", "neoforge-26.1").forEach(::include)
