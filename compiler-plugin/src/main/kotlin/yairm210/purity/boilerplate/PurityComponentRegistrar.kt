package yairm210.purity.boilerplate

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import yairm210.purity.PurityConfig

@AutoService(CompilerPluginRegistrar::class)
class PurityComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) return

        val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        val logging = true
        
        IrGenerationExtension.registerExtension(
            PurityIrGenerationExtension(
                DebugLogger(logging, messageCollector), 
                configuration[KEY_PURITY_CONFIG] ?: PurityConfig()
            )
        )
    }
}
