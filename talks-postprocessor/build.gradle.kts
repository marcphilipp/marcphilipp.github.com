plugins {
    `java-conventions`
    application
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    implementation("info.picocli:picocli:4.7.0")
    implementation(project(":speakerdeck-api"))
}

application {
    mainClass.set("de.marcphilipp.website.TalksPostProcessorMain")
}
