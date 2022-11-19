plugins {
    `java-conventions`
    application
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    implementation(project(":speakerdeck-api"))
}