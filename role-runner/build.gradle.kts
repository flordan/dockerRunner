plugins {
    id("java")
}

group = "com.github.flordan"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Use JUnit test framework.
    testImplementation(libs.junit)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}