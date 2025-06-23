import org.gradle.kotlin.dsl.libs

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform") version libs.versions.kotlin apply false
}

System.setProperty("kotlin.compiler.execution.strategy", "in-process") // For debugging


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://plugins.gradle.org/m2/")
        google()
    }
}

