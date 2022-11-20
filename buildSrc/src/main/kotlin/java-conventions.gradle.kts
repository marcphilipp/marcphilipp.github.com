plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(project.the<JavaToolchainService>().launcherFor(java.toolchain))
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter("5.9.1")
        }
    }
}
