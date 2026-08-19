import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "dev.rustdiagnostics"
version = "0.2.0"

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

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
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
            <p>
                Rust Inline Diagnostics displays Rust errors and warnings directly underneath
                the affected source code in RustRover.
            </p>
        
            <p>
                Instead of hovering over highlighted code to read a diagnostic, you can see
                the message directly in the editor while you work.
            </p>
        
            <h3>Features</h3>
        
            <ul>
                <li>Persistent inline diagnostics for Rust code</li>
                <li>Error and warning severity indicators</li>
                <li>Theme-aware diagnostic colors</li>
                <li>Diagnostics positioned near the affected source code</li>
                <li>Automatically refreshes when RustRover code analysis completes</li>
                <li>Supports multiple diagnostics on the same source line</li>
                <li>Uses RustRover's existing code analysis</li>
                <li>No additional configuration required</li>
            </ul>
        
            <p>
                Rust Inline Diagnostics is especially useful when working with compiler
                and IDE feedback related to ownership, borrowing, mutability, type mismatches,
                traits, and other Rust errors and warnings.
            </p>
            
            <h3>Preview</h3>

            <p>
                <img src="https://raw.githubusercontent.com/JameelSawafta/rustrover-inline-diagnostics/master/screenshots/Screenshot1.png"
                     alt="Rust Inline Diagnostics Preview" />
            </p>
        
            <p>
                <img src="https://raw.githubusercontent.com/JameelSawafta/rustrover-inline-diagnostics/master/screenshots/Screenshot2.png"
                     alt="Rust Inline Diagnostics Preview" />
            </p>
        
            <p>
                <img src="https://raw.githubusercontent.com/JameelSawafta/rustrover-inline-diagnostics/master/screenshots/Screenshot3.png"
                     alt="Rust Inline Diagnostics Preview" />
            </p>
        
            <p>
                <img src="https://raw.githubusercontent.com/JameelSawafta/rustrover-inline-diagnostics/master/screenshots/Screenshot4.png"
                     alt="Rust Inline Diagnostics Preview" />
            </p>
        
            <p>
                <img src="https://raw.githubusercontent.com/JameelSawafta/rustrover-inline-diagnostics/master/screenshots/Screenshot5.png"
                     alt="Rust Inline Diagnostics Preview" />
            </p>
        
            <p>
                The plugin is designed specifically for JetBrains RustRover.
            </p>
        """.trimIndent()

        changeNotes = """
            <h3>Version 0.2.0</h3>
            <ul>
                <li>Improved RustRover compatibility.</li>
                <li>Improved inline diagnostic rendering.</li>
                <li>Added support for newer RustRover releases.</li>
                <li>Updated build configuration for Java 21 bytecode compatibility.</li>
            </ul>
        """.trimIndent()
    }
}