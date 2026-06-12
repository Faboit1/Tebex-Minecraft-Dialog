import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version

fun gitCommitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (process.exitValue() == 0) result else "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

plugins {
    java
    id("com.gradleup.shadow")
    id("net.neoforged.moddev") version "2.0.141"
}

val neoVersion = properties["neo_version"] as String
val modId = properties["mod_id"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

neoForge {
    version = neoVersion

    mods {
        create(modId) {
            sourceSet(sourceSets.getByName("main"))
        }
    }
}

dependencies {
    shadow(project(":sdk"))

    shadow("com.github.cryptomorin:XSeries:9.3.1") {
        isTransitive = false
    }

    compileOnly("dev.dejvokep:boosted-yaml:1.3")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    configurations = listOf(project.configurations.shadow.get())

    relocate("okhttp3", "io.tebex.plugin.libs.okhttp3")
    relocate("okio", "io.tebex.plugin.libs.okio")
    relocate("dev.dejvokep.boostedyaml", "io.tebex.plugin.libs.boostedyaml")
    relocate("org.jetbrains.annotations", "io.tebex.plugin.libs.jetbrains")
    relocate("kotlin", "io.tebex.plugin.libs.kotlin")
    relocate("com.google.gson", "io.tebex.plugin.libs.gson")
    minimize()

    archiveFileName.set("tebex-${project.name}-${rootProject.version}-${gitCommitHash()}.jar")
}

tasks.named("jar") {
    dependsOn("shadowJar")
}
