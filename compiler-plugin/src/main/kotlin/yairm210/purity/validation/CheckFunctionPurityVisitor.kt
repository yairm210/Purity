@file:OptIn(UnsafeDuringIrConstructionAPI::class)
package yairm210.purity.validation

import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import yairm210.purity.PurityConfig


fun getLocationForExpression(function: IrFunction, expression: IrElement) = 
    getLocationForExpression(function.fileEntry, expression)

fun getLocationForExpression(
    fileEntry: IrFileEntry,
    expression: IrElement
): CompilerMessageLocation {
    val lineAndColumn = fileEntry.getLineAndColumnNumbers(expression.startOffset)
    return CompilerMessageLocation.create(
        path = fileEntry.name,
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
    private var hasErrored = false

    val hasExpectCompileErrorAnnotation = function.hasAnnotation(FqName("yairm210.purity.annotations.TestExpectCompileError"))
    private val errorSeverity = if (hasExpectCompileErrorAnnotation) CompilerMessageSeverity.WARNING else CompilerMessageSeverity.ERROR
    
    fun actualFunctionPurity(): FunctionPurity {
        return when {
            isPure -> FunctionPurity.Pure
            isReadonly -> FunctionPurity.Readonly
            else -> FunctionPurity.None
        }
    }
    
    private fun report(message: String, element: IrElement) {
        messageCollector.report(
            errorSeverity,
            message,
            location = getLocationForExpression(function, element)
        )
        hasErrored = true
    }
    
    private fun varCreatedInFunction(varValueDeclaration: IrValueDeclaration): Boolean {
        // If the variable is created in this function that's ok
        // Contains, because if e.g. we create a sequence{} in a function and define the variable in the sequence, it's the parent
        return function in varValueDeclaration.parents
    }

    // Iterate over IR tree and warn on each var set where the var is not created within this function
    override fun visitSetValue(expression: IrSetValue, data: Unit) {
        if (declaredFunctionPurity == FunctionPurity.None
                || expression.symbol.owner.hasAnnotation(FqName("yairm210.purity.annotations.Cache"))){
            super.visitSetValue(expression, data)
            return
        }
        
        // Not sure if we can assume owner is set at this point :think:
        val varValueDeclaration: IrValueDeclaration = expression.symbol.owner
        
        
        if (varValueDeclaration is IrVariable && varValueDeclaration.isVar
            && !varCreatedInFunction(varValueDeclaration)) { 
            isReadonly = false
            isPure = false

            report(
                "Function \"${function.name}\" is marked as $declaredFunctionPurity but sets variable \"${varValueDeclaration.name}\"",
                expression
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
                report(
                    "Function \"${function.name}\" is marked as $declaredFunctionPurity but gets variable \"${varValueDeclaration.name}\"",
                    expression 
                )
            }
        }
        super.visitGetValue(expression, data)
    }
    
    override fun visitCall(expression: IrCall, data: Unit) {

        checkCalledFunctionPurity(expression)
        checkMarkedParameters(expression)

        super.visitCall(expression, data) 
    }

    
    private fun checkCalledFunctionPurity(expression: IrCall) {
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
            val expressionDispatchReceiver = expression.dispatchReceiver
            return expressionDispatchReceiver is IrGetValue &&
                    // is val
                    expressionDispatchReceiver.symbol.owner.let { it is IrVariable && !it.isVar } &&
                    // Is declared in our function
                    expressionDispatchReceiver.symbol.owner.parent == function
            // Val is set to result of constructor - TODO
        }
        
        fun receiverHasAnnotation(annotation: String): Boolean {
            return (expression.dispatchReceiver ?: expression.extensionReceiver)
                ?.let { representsAnnotationBearer(it, annotation) } == true
        }

        val calledFunctionPurity = when {
            // Pure function
            ExpectedFunctionPurityChecker.isMarkedAsPure(calledFunction, purityConfig)
                    || (callerIsConstructedInOurFunction() && ExpectedFunctionPurityChecker.classMatches(
                calledFunction,
                wellKnownInternalStateClasses
            ))
                -> FunctionPurity.Pure

            // Readonly functions on Immutable vals, are considered pure
            receiverHasAnnotation("yairm210.purity.annotations.Immutable")
                    && ExpectedFunctionPurityChecker.isReadonly(calledFunction, purityConfig)
                -> FunctionPurity.Pure

            // All functions on LocalState variables are considered pure
            receiverHasAnnotation("yairm210.purity.annotations.LocalState")
                    || receiverHasAnnotation("yairm210.purity.annotations.Cache")
                -> FunctionPurity.Pure

            calledFunction.name.asString() == "invoke"
                    && expression.dispatchReceiver?.type?.isFunction() == true
                -> FunctionPurity.Pure

            ExpectedFunctionPurityChecker.isReadonly(calledFunction, purityConfig) -> FunctionPurity.Readonly

            else -> FunctionPurity.None
        }


        if (calledFunctionPurity < FunctionPurity.Pure) isPure = false
        if (calledFunctionPurity < FunctionPurity.Readonly) isReadonly = false

        if (declaredFunctionPurity > calledFunctionPurity) {
            report(
                "Function \"${function.name}\" is marked as $declaredFunctionPurity " +
                        "but calls non-$declaredFunctionPurity function \"${expression.symbol.owner.fqNameForIrSerialization}\"",
                expression
            )
        }
    }

    private fun representsAnnotationBearer(irExpression: IrExpression, annotation: String): Boolean {
        if (irExpression is IrGetValue) { // local function variable
            return irExpression.symbol.owner.hasAnnotation(FqName(annotation))
        }
        if (irExpression is IrCall) {
            return irExpression.symbol.owner.let {
                it == it.correspondingPropertySymbol?.owner?.getter // A getter..
                        // ... for a property that is immutable
                        && it.correspondingPropertySymbol?.owner?.hasAnnotation(FqName(annotation)) == true
            }
        }
        return false
    }


    private fun checkMarkedParameters(expression: IrCall) {
            
        val calledFunction = expression.symbol.owner
        
        for ((parameter, parameterExpression) in expression.getAllArgumentsWithIr()) {
            val parameterPurity = when {
                parameter.hasAnnotation(FqName("yairm210.purity.annotations.Pure")) -> FunctionPurity.Pure
                parameter.hasAnnotation(FqName("yairm210.purity.annotations.Readonly")) -> FunctionPurity.Readonly
                else -> FunctionPurity.None
            }

            if (parameterPurity == FunctionPurity.None) continue

            if (parameterExpression is IrFunctionExpression) { // lambda expression
                // Must be readonly
                // We instantiate a new checkFunctionPurityVisitor to check the lambda.
                // We don't use the current visitor because the lambda may have a different purity than the entire function,
                //  e.g  a readonly function that calls a pure function, sending a pure lambda.


                // If this functions is already on the parameter purity level or higher, we're already checking!
                // There is still a case where we will check things doubly:
                // If the parent function is readonly, and the parameter is pure, we'll create a Pure visitor
                // That means that readonly violations in the lambda will be raised by both the parent and the child
                if (declaredFunctionPurity < parameterPurity) {
                    val visitor = CheckFunctionPurityVisitor(
                        function = parameterExpression.function,
                        declaredFunctionPurity = parameterPurity,
                        messageCollector = messageCollector,
                        purityConfig = purityConfig,
                    )

                    // If there are problems, this will raise them as-is
                    parameterExpression.function.accept(visitor, Unit)
                }
                continue
            }
            
            if (parameterExpression is IrGetValue){
                val possibleAnnotations = when (parameterPurity) {
                    FunctionPurity.Pure -> listOf("yairm210.purity.annotations.Pure")
                    FunctionPurity.Readonly -> listOf("yairm210.purity.annotations.Readonly", "yairm210.purity.annotations.Pure")
                    FunctionPurity.None -> listOf()
                }
                
                // If we're passing a @Readonly to @Readonly or a @Pure to @Pure, that's fine
                val hasAcceptableAnnotation = possibleAnnotations.any { 
                    parameterExpression.symbol.owner.hasAnnotation(FqName(it)) 
                }
                
                if (!hasAcceptableAnnotation) {
                    report(
                        "Function \"${function.name}\" calls \"${calledFunction.fqNameForIrSerialization}\" " +
                                "with parameter \"${parameter.name}\" that is marked as $parameterPurity, but the value sent is not marked as @$parameterPurity.",
                        expression
                    )
                }
                continue
            }
            
            report(
                "Function \"${function.name}\" calls \"${calledFunction.fqNameForIrSerialization}\" " +
                        "with parameter \"${parameter.name}\" that is marked as $parameterPurity, but the value sent is not a lambda function.",
                expression
            )
        }
    }

    override fun visitElement(element: IrElement, data: Unit) {
        element.acceptChildren(this, data)
    }
}