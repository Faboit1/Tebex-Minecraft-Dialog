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
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        gradlePluginPortal()
    }
}

plugins {
    //id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "TebexPlugin"

listOf(
    "sdk",
    "bukkit"
).forEach(::include)
