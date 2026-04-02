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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "TebexPlugin"

listOf("sdk", "bukkit", "bungeecord", "velocity", "folia", "fabric-1.21.1", "fabric-1.21.4", "fabric-1.21.5", "fabric-1.21.6", "fabric-1.21.7", "fabric-1.21.11", "fabric-26.1").forEach(::include)
