package yairm210.purity.validation

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.parentEnumClassOrNull
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import yairm210.purity.PurityConfig
import yairm210.purity.boilerplate.DebugLogger


internal class PurityElementTransformer(
    private val pluginContext: IrPluginContext,
    private val debugLogger: DebugLogger,
    private val purityConfig: PurityConfig,
) : IrElementTransformerVoidWithContext() {
    
    // These are created behind the scenes for every class, don't warn for them
    private val autogeneratedFunctions = setOf(
        "equals",
        "hashCode",
        "toString"
    )
    private val enumAutogeneratedFunctions = setOf(
        "values",
        "valueOf",
        "compareTo",
        "clone"
    )
    
    private fun isAutogeneratedFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return autogeneratedFunctions.contains(name) 
                || name.startsWith('<') // auto-generated functions like <init>, <get-name>, <set-name>
                || function.parentEnumClassOrNull != null && (name in enumAutogeneratedFunctions) // Enum values function
                || function.parentClassOrNull?.isData == true && ExpectedFunctionPurityChecker.componentRegex.matches(name) // componentN functions for data classes
                || function.parentClassOrNull?.isData == true && name == "copy" // copy function for data classes

    }
    
    private fun isSuppressed(declaration: IrSimpleFunction): Boolean {
        val suppressAnnotation = declaration.annotations.findAnnotation(FqName("kotlin.Suppress"))
            ?: return false
        
        // getAnnotationArgumentValue does not work for varargs, so we find the vararg
        //.flatmap{} instead of .valueArguments[0] because Suppress can be called with zero parameters also -_-
        @Suppress("UNCHECKED_CAST")
        val suppressParameters: List<String> = suppressAnnotation.valueArguments.flatMap { (it as IrVarargImpl).elements }
            .mapNotNull{it as? IrConst<String>}.map { it.value }

        return suppressParameters.contains("purity")
    }

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.visibility != DescriptorVisibilities.PRIVATE && declaration.hasAnnotation(FqName("yairm210.purity.annotations.Cache"))){
            val message = "Variable \"${declaration.name}\" is marked as \"@Cache\", but is public - this annotation is reserved for private variables"
            
            debugLogger.messageCollector.report(
                CompilerMessageSeverity.ERROR, message,
                location = getLocationForExpression(declaration.fileEntry, declaration)
            )
        }
        return super.visitPropertyNew(declaration)
    }
    
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (isAutogeneratedFunction(declaration)) return super.visitSimpleFunction(declaration)

        // Skip interface/abstract functions that are not implemented
        if (declaration.body == null) return super.visitSimpleFunction(declaration)

        if (isSuppressed(declaration)) return super.visitSimpleFunction(declaration)

        val functionDeclaredPurity = when {
            ExpectedFunctionPurityChecker.isMarkedAsPure(declaration, purityConfig) -> FunctionPurity.Pure
            ExpectedFunctionPurityChecker.isReadonly(declaration, purityConfig) -> FunctionPurity.Readonly
            else -> FunctionPurity.None
        }
        val messageCollector = debugLogger.messageCollector

        val visitor = CheckFunctionPurityVisitor(declaration, functionDeclaredPurity, messageCollector, purityConfig)
        declaration.accept(visitor, Unit)

        val actualPurity = visitor.actualFunctionPurity()

        if (visitor.hasExpectCompileErrorAnnotation) { // opposite land - fail is success, success is fail
            if (actualPurity >= functionDeclaredPurity)
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Function \"${declaration.name}\" should fail on purity checks, but succeeds!",
                    location = getLocationForExpression(declaration, declaration)
                )

            return super.visitSimpleFunction(declaration)
        }

        val shouldWarnForPossibleAnnotations =
            functionDeclaredPurity < actualPurity
                && purityConfig.warnOnPossibleAnnotations
                && !isAutogeneratedFunction(declaration)
                && !declaration.overriddenSymbols.any() // not inherited
                && declaration.parentClassOrNull?.isInterface != true
        
        if (shouldWarnForPossibleAnnotations) {
            // if equal, no message; If less that declared, we already warn for each individual violation
            val message = when (actualPurity) {
                FunctionPurity.Pure -> "Function \"${declaration.name}\" can be marked with @Pure to indicate it is pure"
                FunctionPurity.Readonly -> "Function \"${declaration.name}\" can be marked with @Readonly to indicate it is readonly"
                else -> throw Exception("Unexpected function purity: $actualPurity")
            }

            debugLogger.messageCollector.report(
                CompilerMessageSeverity.WARNING, message,
                location = getLocationForExpression(declaration, declaration)
            )
        }

        return super.visitSimpleFunction(declaration)
    }

}