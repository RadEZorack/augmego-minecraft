plugins {
    id("fabric-loom") version "1.14.10"
    `maven-publish`
}

base {
    archivesName.set(property("archives_base_name") as String)
}

version = property("mod_version") as String
group = property("maven_group") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    implementation(project(":shared:avatar-core"))
}

tasks.processResources {
    val modId = project.property("mod_id")
    val modName = project.property("mod_name")
    val versionValue = project.version

    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "mod_id" to modId,
                "mod_name" to modName,
                "version" to versionValue
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
