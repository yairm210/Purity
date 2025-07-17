import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("org.jetbrains.kotlin.multiplatform") version libs.versions.kotlin
    id("com.vanniktech.maven.publish") version("0.32.0")
    signing
}

group = "io.github.yairm210"
version = "0.0.25"

kotlin {
    sourceSets{
        val commonMain by getting {
        }
    }
    jvm()
    jvmToolchain(8) // test plugin compatibility to older jvm
}

mavenPublishing {
    coordinates(group.toString(), "purity-annotations", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications() // Comment out for local publishing if you don't have a GPG key set up

    pom {
        name = "Purity Compiler Plugin"
        description = "Annotations for the Purity Compiler Plugin, which allows you to mark Kotlin functions as pure and readonly"
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