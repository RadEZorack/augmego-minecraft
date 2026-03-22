plugins {
    id("fabric-loom") version "1.14.10"
    `maven-publish`
}

evaluationDependsOn(":shared:avatar-core")

base {
    archivesName.set(property("archives_base_name") as String)
}

version = property("mod_version") as String
group = property("maven_group") as String

val fabricApiCoordinates = "net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}"

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
    modImplementation(fabricApiCoordinates)
    modImplementation(include("de.keksuccino:mcef-fabric:2.2.0-1.21.11")!!)
    implementation("de.javagl:jgltf-model:2.0.4")
    implementation("de.javagl:jgltf-impl-v1:2.0.4")
    implementation("de.javagl:jgltf-impl-v2:2.0.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")
    include("de.javagl:jgltf-model:2.0.4")
    include("de.javagl:jgltf-impl-v1:2.0.4")
    include("de.javagl:jgltf-impl-v2:2.0.4")
    include("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    include("com.fasterxml.jackson.core:jackson-core:2.13.1")
    include("com.fasterxml.jackson.core:jackson-annotations:2.13.4")

    implementation(project(":shared:avatar-core"))
}

val installFabricApiToMods by tasks.registering(Copy::class) {
    description = "Copies the Fabric API jar into minecraft/mods for local/server installs."
    group = "distribution"

    val fabricApiJar = configurations.detachedConfiguration(dependencies.create(fabricApiCoordinates)).apply {
        isTransitive = false
    }

    from(fabricApiJar)
    into(rootProject.layout.projectDirectory.dir("minecraft/mods"))
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

tasks.jar {
    val avatarCoreJar = project(":shared:avatar-core").tasks.named("jar")
    dependsOn(avatarCoreJar)
    from(avatarCoreJar.map { zipTree(it.outputs.files.singleFile) })
}

tasks.build {
    dependsOn(installFabricApiToMods)
}
