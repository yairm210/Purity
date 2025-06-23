
includeBuild("gradle-plugin")
includeBuild("compiler-plugin"){
    dependencySubstitution { // for lib, even though it gets purity-compiler-plugin dependency via gradle plugin, will still use local compiler-plugin
        substitute(module("io.github.yairm210:purity-compiler-plugin"))
            .using(project(":"))
    }
}

include(":lib")

// Allow plugins from local - required for lib to get the gradle plugin from our maven local
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

