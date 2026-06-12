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
    id("net.neoforged.gradle.userdev") version "7.1.36"
}

val neoVersion = properties["neo_version"] as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("net.neoforged:neoforge:$neoVersion")
    shadow(project(":sdk"))
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

    archiveBaseName.set("tebex-${project.name}")
    archiveVersion.set("${rootProject.version}-${gitCommitHash()}")
    archiveClassifier.set("")
}
