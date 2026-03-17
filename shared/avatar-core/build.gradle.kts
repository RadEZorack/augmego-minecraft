plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
}

dependencies {
    implementation("de.javagl:jgltf-model:2.0.4")
    implementation("de.javagl:jgltf-model-builder:2.0.4")
}
