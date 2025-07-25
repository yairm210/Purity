import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version("2.0.0")
    kotlin("kapt") version("2.0.0")
    id("com.vanniktech.maven.publish") version("0.32.0")
    signing
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.google.com")
        maven("https://plugins.gradle.org/m2/")
        google()
    }
}

group = "io.github.yairm210"
version = "0.0.38"

mavenPublishing {
    coordinates(group.toString(), "purity-compiler-plugin", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications() // Comment out for local publishing if you don't have a GPG key set up

    pom {
        name = "Purity Compiler Plugin"
        description = "The Compiler plugin for the Purity, which allows you to mark Kotlin functions as pure and readonly"
        inceptionYear = "2025"
        url = "https://github.com/yairm210/purity"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "yairm210"
                name = "Yair Morgenstern"
                url = "https://github.com/yairm210"
            }
        }
        scm {
            url = "https://github.com/yairm210/purity"
            connection = "scm:git:git://github.com/yairm210/purity.git"
            developerConnection = "scm:git:ssh://git@github.com/yairm210/purity.git"
        }
    }
}


val autoService = "1.1.1"
dependencies {
    compileOnly("com.google.auto.service:auto-service:$autoService")
    kapt("com.google.auto.service:auto-service:$autoService")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.0")
    testImplementation("dev.zacsweers.kctfork:core:0.4.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation(kotlin("reflect"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

//./gradlew clean :lib:compileKotlinJvm --no-daemon -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process" -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"
