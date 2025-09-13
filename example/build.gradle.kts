plugins {
    kotlin("jvm")
    id("io.github.yairm210.purity-plugin") version "1.3.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":annotations"))
}

kotlin {
    jvmToolchain(8)
}
