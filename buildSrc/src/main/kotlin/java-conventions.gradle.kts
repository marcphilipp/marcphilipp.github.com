plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter("5.9.1")
        }
    }
}
