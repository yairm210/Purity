package yairm210.purity.boilerplate

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import yairm210.purity.PurityConfig

val enabled = "enabled"
val warnOnPossibleAnnotations = "warnOnPossibleAnnotations"
val wellKnownPureClasses = "wellKnownPureClasses"
val wellKnownPureFunctions = "wellKnownPureFunctions"
val wellKnownReadonlyClasses = "wellKnownReadonlyClasses"
val wellKnownReadonlyFunctions = "wellKnownReadonlyFunctions"
val wellKnownInternalStateClasses = "wellKnownInternalStateClasses"

@AutoService(CommandLineProcessor::class) // don't forget!
class PurityCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "purityPlugin"
    
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = enabled, valueDescription = "<true|false>",
            description = "whether to enable the plugin or not"
        ),
        CliOption(
            optionName = warnOnPossibleAnnotations, valueDescription = "<true|false>",
            description = "whether to warn when annotations can be added/changed"
        ),
        CliOption(
            optionName = wellKnownPureClasses, valueDescription = "<fully qualified class names delimited by underscores>",
            description = "A list of fully qualified class names, all functions of which are pure functions",
            required = false
        ),
        CliOption(
            optionName = wellKnownPureFunctions, valueDescription = "<fully qualified function names delimited by underscores>",
            description = "A list of fully qualified function names that are pure functions",
            required = false
        ),
        CliOption(
            optionName = wellKnownReadonlyClasses, valueDescription = "<fully qualified class names delimited by underscores>",
            description = "A list of fully qualified class names, all functions of which are readonly functions",
            required = false
        ),
        CliOption(
            optionName = wellKnownReadonlyFunctions, valueDescription = "<fully qualified function names delimited by underscores>",
            description = "A list of fully qualified function names that are readonly functions",
            required = false
        ),
        CliOption(
            optionName = wellKnownInternalStateClasses, valueDescription = "<fully qualified class names delimited by underscores>",
            description = "A list of fully qualified class names that alter only internal state",
            required = false
        ),
    )
    
    private fun stringToSet(string:String) = string.split("_").map { it.trim() }.toSet()

    private fun getConfig(configuration: CompilerConfiguration): PurityConfig {
        val currentValue = configuration.get(KEY_PURITY_CONFIG)
        if (currentValue != null) return currentValue
        val newConfig = PurityConfig()
        configuration.put(KEY_PURITY_CONFIG, newConfig)
        return newConfig
    }
    
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        enabled -> configuration.put(KEY_ENABLED, value.toBoolean())
        warnOnPossibleAnnotations -> getConfig(configuration).warnOnPossibleAnnotations = value.toBoolean()
        wellKnownPureClasses -> getConfig(configuration).wellKnownPureClassesFromUser = stringToSet(value)
        wellKnownPureFunctions -> getConfig(configuration).wellKnownPureFunctionsFromUser = stringToSet(value)
        wellKnownReadonlyFunctions -> getConfig(configuration).wellKnownReadonlyFunctionsFromUser = stringToSet(value)
        wellKnownInternalStateClasses -> getConfig(configuration).wellKnownInternalStateClassesFromUser = stringToSet(value)
        wellKnownReadonlyClasses -> getConfig(configuration).wellKnownReadonlyClassesFromUser = stringToSet(value)
        else -> throw IllegalArgumentException("Unknown option: ${option.optionName}")
    }
}

val KEY_ENABLED = CompilerConfigurationKey<Boolean>(enabled)
val KEY_PURITY_CONFIG = CompilerConfigurationKey<PurityConfig>("purityConfig")
