import yairm210.purity.PurityConfiguration

plugins {
    id("org.jetbrains.kotlin.multiplatform") version libs.versions.kotlin
    id("io.github.yairm210.purity-plugin") version "1.0.3" // This is overridden in settings so the version doesn't matter
}


configure<PurityConfiguration> {
    enabled = true
    warnOnPossibleAnnotations = false
    wellKnownPureClasses = setOf("kotlin.ranges.IntRange")
    wellKnownPureFunctions = setOf("hi","bob")
    wellKnownReadonlyFunctions = setOf("java.util.EnumMap.get")
}


kotlin {
    jvm()
    jvmToolchain(8) // test plugin compatibility to older jvm
//    linuxX64("linux")
//    js()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }

//        val jsMain by getting {
//
//            dependencies {
//
//            }
//        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":annotations"))
            }
        }
//        val linuxMain by getting {
//
//        }

    }
}

