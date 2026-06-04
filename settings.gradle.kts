pluginManagement {
    repositories {
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Sponge"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
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

listOf(
    "sdk",
    "bukkit",
    "bungeecord",
    "velocity",
    "folia",
    "fabric-26.1",
    "neoforge-26.1",
    "forge-26.1"
).forEach(::include)
