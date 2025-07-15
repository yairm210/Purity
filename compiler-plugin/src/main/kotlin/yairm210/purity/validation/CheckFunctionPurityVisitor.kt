@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import yairm210.purity.PurityConfig


fun getLocationForExpression(
    function: IrFunction,
    expression: IrElement
): CompilerMessageLocation {
    val lineAndColumn = function.fileEntry.getLineAndColumnNumbers(expression.startOffset)
    return CompilerMessageLocation.create(
        path = function.fileEntry.name,
        line = lineAndColumn.line + 1, // Convert to 1-indexed
        column = lineAndColumn.column + 1, // Convert to 1-indexed
        lineContent = null
    )!!
}

/** Checks all declarations of a specific function.
 * Warns every time a var is set a value, or an unpure function is called.
 * Vars that are created within the function are OK to set */
class CheckFunctionPurityVisitor(
    private val function: IrFunction,
    private val declaredFunctionPurity: FunctionPurity,
    private val messageCollector: MessageCollector,
    private val purityConfig: PurityConfig,
    ) : IrElementVisitor<Unit, Unit> { // Returns whether this is an acceptable X function
    private var isReadonly = true
    private var isPure = true
    
    fun actualFunctionColoring(): FunctionPurity {
        return when {
            isPure -> FunctionPurity.Pure
            isReadonly -> FunctionPurity.Readonly
            else -> FunctionPurity.None
        }
    }
    
    private fun varCreatedInFunction(varValueDeclaration: IrValueDeclaration): Boolean {
        // If the variable is created in this function that's ok
        // Contains, because if e.g. we create a sequence{} in a function and define the variable in the sequence, it's the parent
        return function in varValueDeclaration.parents
    }

    // Iterate over IR tree and warn on each var set where the var is not created within this function
    override fun visitSetValue(expression: IrSetValue, data: Unit) {
        // Not sure if we can assume owner is set at this point :think:
        val varValueDeclaration: IrValueDeclaration = expression.symbol.owner
        
        
        if (varValueDeclaration is IrVariable && varValueDeclaration.isVar
            && !varCreatedInFunction(varValueDeclaration)) { 
            isReadonly = false
            isPure = false

            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Function \"${function.name}\" is marked as $declaredFunctionPurity but sets variable \"${varValueDeclaration.name}\"",
                location = getLocationForExpression(function, expression)
            )
        }
        super.visitSetValue(expression, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit) {
        val varValueDeclaration: IrValueDeclaration = expression.symbol.owner
        
        // If the variable is created in this function that's ok
        if (varValueDeclaration is IrVariable && varValueDeclaration.isVar
            && !varCreatedInFunction(varValueDeclaration)) {
            isPure = false

            if (declaredFunctionPurity == FunctionPurity.Pure) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Function \"${function.name}\" is marked as $declaredFunctionPurity but gets variable \"${varValueDeclaration.name}\"",
                    location = getLocationForExpression(function, expression) 
                )
            }
        }
        super.visitGetValue(expression, data)
    }
    
    override fun visitCall(expression: IrCall, data: Unit) {
        // Only accept calls to functions marked as pure or readonly
        val calledFunction = expression.symbol.owner
        
        /** Great in theory, thorny in practice...
         *  We currently don't have a good way to validate that the class's constructor was called from our function
         *  And this isn't just redeclaring a local val to an external one - see alterExternallyDeclaredInnerStateClass in SampleJvm
         *  Keeping this in case anyone else has a good idea of how to implement it because the theory is sound
         *  When this works, uncomment correctLocalStateNonChainingPure
         *  */
        fun callerIsConstructedInOurFunction(): Boolean {
            return false
            //  
            return expression.dispatchReceiver is IrGetValue &&
                    // is val
                    (expression.dispatchReceiver as IrGetValue).symbol.owner.let { it is IrVariable && !it.isVar }
                    // Is declared in our function
                    (expression.dispatchReceiver as IrGetValue).symbol.owner.parent == function
                    // Val is set to result of constructor - TODO
        }
        
        val calledFunctionPurity =  when {
            FunctionPurityChecker.isMarkedAsPure(calledFunction, purityConfig) 
                    || (callerIsConstructedInOurFunction() && FunctionPurityChecker.classMatches(calledFunction, wellKnownInternalStateClasses))
                -> FunctionPurity.Pure
            FunctionPurityChecker.isReadonly(calledFunction, purityConfig) -> FunctionPurity.Readonly
            else -> FunctionPurity.None
        }
        
        
        if (calledFunctionPurity < FunctionPurity.Pure) isPure = false
        if (calledFunctionPurity < FunctionPurity.Readonly) isReadonly = false
        
        if (declaredFunctionPurity > calledFunctionPurity) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Function \"${function.name}\" is marked as $declaredFunctionPurity " +
                        "but calls non-$declaredFunctionPurity function \"${expression.symbol.owner.fqNameForIrSerialization}\"",
                location = getLocationForExpression(function, expression)
            )
        }
        
        super.visitCall(expression, data) 
    }

    override fun visitElement(element: IrElement, data: Unit) {
        element.acceptChildren(this, data)
    }
}