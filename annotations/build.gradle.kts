import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform") version libs.versions.kotlin
    id("com.vanniktech.maven.publish") version("0.32.0")
    signing
}

group = "io.github.yairm210"
version = "1.3.4"
val isLocalPublish = gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal") }
val skipSigning = (findProperty("skipSigning") as String?)?.toBooleanStrictOrNull() == true

kotlin {
    sourceSets{
        val commonMain by getting {
        }
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    jvmToolchain((findProperty("jvmToolchainVersion") as String?)?.toInt() ?: 17)

}

mavenPublishing {
    coordinates(group.toString(), "purity-annotations", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

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

tasks.withType<Sign>().configureEach {
    onlyIf { !isLocalPublish && !skipSigning }
}

tasks.matching { it.name.startsWith("sign") && it.name.endsWith("Publication") }.configureEach {
    onlyIf { !isLocalPublish && !skipSigning }
}
