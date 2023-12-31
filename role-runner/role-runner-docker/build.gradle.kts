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

    implementation(project(":role-runner"))

    //docker
    implementation("com.github.docker-java:docker-java-core:3.3.2")
    implementation("com.github.docker-java:docker-java:3.3.2")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.2")
}
