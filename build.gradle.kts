plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "dev.rustdiagnostics"
version = "0.1.2"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        rustRover("2026.2.1")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

intellijPlatform {
    pluginConfiguration {
        name = "Rust Inline Diagnostics"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "251"
        }

        vendor {
            name = "Jameel Sawafta"
        }

        description = """
            Displays Rust diagnostics directly inside the editor
            without requiring you to hover over errors and warnings.

            Features:
            • Persistent inline Rust diagnostics
            • Error and warning severity indicators
            • Theme-aware diagnostic colors
            • Diagnostics positioned below the affected source line
        """.trimIndent()
    }
}