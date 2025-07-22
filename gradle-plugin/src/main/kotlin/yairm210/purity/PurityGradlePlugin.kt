package yairm210.purity

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

open class PurityConfiguration {
    var enabled: Boolean = true
    /** If true, adds warnings where @Readonly or @Pure annotations can be added */
    var warnOnPossibleAnnotations: Boolean = false
    /** Fully-qualified names of classes where all functions are pure */
    var wellKnownPureClasses = setOf<String>()
    /** Fully-qualified names of pure functions */
    var wellKnownPureFunctions = setOf<String>()
    /** Fully-qualified names of readonly functions */
    var wellKnownReadonlyFunctions = setOf<String>()
}

@Suppress("unused")
class PurityGradlePlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val COMPILER_PLUGIN_GROUP_NAME = "io.github.yairm210"
        const val ARTIFACT_NAME = "purity-compiler-plugin"
        const val VERSION_NUMBER = "0.0.34"
    }

    private var gradleExtension : PurityConfiguration = PurityConfiguration()
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        gradleExtension = kotlinCompilation.target.project.extensions.findByType(PurityConfiguration::class.java) ?: PurityConfiguration()

        return kotlinCompilation.target.project.provider {
            val options = mutableListOf(
                // TODO: Find a way to sync the key names between projects - other side is in PurityCommandLineProcessor
                SubpluginOption("enabled", gradleExtension.enabled.toString()),
                SubpluginOption("warnOnPossibleAnnotations", gradleExtension.warnOnPossibleAnnotations.toString()),
                SubpluginOption("wellKnownPureClasses", gradleExtension.wellKnownPureClasses.joinToString("_")),
                SubpluginOption("wellKnownPureFunctions", gradleExtension.wellKnownPureFunctions.joinToString("_")),
                SubpluginOption("wellKnownReadonlyFunctions", gradleExtension.wellKnownReadonlyFunctions.joinToString("_")),
            )
            options
        }
    }

    override fun apply(target: Project) {
        target.extensions.create(
            "purity",
            PurityConfiguration::class.java
        )
        super.apply(target)
    }

    override fun getCompilerPluginId(): String = "purityPlugin"

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return true
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = COMPILER_PLUGIN_GROUP_NAME,
        artifactId = ARTIFACT_NAME,
        version = VERSION_NUMBER // remember to bump this version before any release!
    )

}
