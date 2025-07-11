package yairm210.purity.boilerplate

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import yairm210.purity.PurityConfig
import yairm210.purity.validation.PurityElementTransformer

internal class PurityIrGenerationExtension(private val debugLogger: DebugLogger,
                                           private val purityConfig: PurityConfig,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(
            PurityElementTransformer(pluginContext, debugLogger, purityConfig), null)
    }
}