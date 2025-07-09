@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package yairm210.purity.transform

import yairm210.purity.DebugLogger
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.parentEnumClassOrNull
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

// All the functions of these are readonly
val wellKnownReadonlyClasses = setOf(
    "kotlin.String",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.Collection",
    "kotlin.sequences.Sequence",
    "kotlin.text.Regex",
    "kotlin.text.MatchResult",
    "kotlin.sequences.Sequence"
)

val wellKnownPureClasses = setOf(
    "kotlin.Int",
    "kotlin.Long",
    "kotlin.Float",
    "kotlin.Double",
    "kotlin.Boolean",
    "kotlin.Char",
    "kotlin.String",

    "kotlin.ranges.IntRange",
    "kotlin.ranges.LongRange",
    "kotlin.ranges.CharRange",
    "kotlin.ranges.FloatRange",
    "kotlin.ranges.DoubleRange",
)

val wellKnownReadonlyFunctions = setOf(
    "java.util.EnumMap.get",
    "java.util.HashMap.get",
    "java.util.LinkedHashMap.get",
)

val wellKnownPureFunctions = setOf(
    "kotlin.internal.ir.CHECK_NOT_NULL", // AKA !!
    // Alas, not all of kotlin.collections is pure... 
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mapOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",
)

val wellKnownPureFunctionsPrefixes = listOf(
    "kotlin.sequences.",
    "kotlin.text."
)

/** Classes that hold state internally.
 * This means that if this function created that class, and it does not leak, it can call all functions on it and be considered pure
*/
val wellKnownInternalStateClasses = setOf(
    "kotlin.collections.ArrayList",
    "kotlin.collections.HashMap",
    "kotlin.collections.LinkedHashMap",
    "kotlin.collections.HashSet",
    "kotlin.collections.LinkedHashSet",
    "kotlin.text.StringBuilder",
    "java.lang.StringBuilder",
    "java.util.EnumMap",
    "java.util.HashMap",
    "java.util.LinkedHashMap",
    "java.util.ArrayList",
    "java.util.HashSet",
    "java.util.LinkedHashSet",
    "java.util.concurrent.ConcurrentHashMap",
)
    
fun classMatches(function: IrFunction, wellKnownClasses: Set<String>): Boolean {
    val parentClassIdentifier = function.parent.fqNameForIrSerialization.asString()
    return parentClassIdentifier in wellKnownClasses
}


fun isMarkedAsPure(function: IrFunction, wellKnownPureClassesFromUser: Set<String>, wellKnownPureFunctionsFromUser: Set<String>): Boolean {
    // Marked by @Contract(pure = true)
    val pure = function.getAnnotationArgumentValue<Boolean>(FqName("org.jetbrains.annotations.Contract"), "pure")
    if (pure == true) return true
    
    val fullyQualifiedClassName = function.parent.fqNameForIrSerialization.asString()
    if (fullyQualifiedClassName in wellKnownPureClasses) return true
    if (fullyQualifiedClassName in wellKnownPureClassesFromUser) return true
    
    val fullyQualifiedFunctionName = function.fqNameForIrSerialization.asString()
    if (fullyQualifiedFunctionName in wellKnownPureFunctions) return true
    if (fullyQualifiedFunctionName in wellKnownPureFunctionsFromUser) return true
    if (wellKnownPureFunctionsPrefixes.any { fullyQualifiedFunctionName.startsWith(it) }) return true
    
    // Simple values like int + int -> plus(int, int), are marked thus
    val constEvaluation = function.getAnnotation(FqName("kotlin.internal.IntrinsicConstEvaluation"))
    if (constEvaluation != null) return true
    
    if (isSingleStatementReturnPure(function)) return true

    return false
}


fun isReadonly(function: IrFunction, wellKnownReadonlyFunctionsFromUser: Set<String>): Boolean {
    // Marked by @Contract(pure = true)
    val contractValue = function.getAnnotationArgumentValue<String>(FqName("org.jetbrains.annotations.Contract"), "value")
    if (contractValue == "readonly") return true
    
    if (function.name.asString().startsWith("<get-")) return true // Autogenerated function for classless variables
    
    val fullyQualifiedClassName = function.parent.fqNameForIrSerialization.asString()
    if (fullyQualifiedClassName in wellKnownReadonlyClasses) return true
    // TODO: Do we need user-inputted readonly classes?

    val fullyQualifiedFunctionName = function.fqNameForIrSerialization.asString()
    if (fullyQualifiedFunctionName in wellKnownReadonlyFunctions) return true
    if (fullyQualifiedFunctionName in wellKnownReadonlyFunctionsFromUser) return true
    
    if (isSingleStatementReturnReadonly(function)) return true

    return false
}

/** For convenience - single-declaration functions like "fun getX() = x" are readonly */
private fun isSingleStatementReturnReadonly(function: IrFunction): Boolean {
    val body = function.body ?: return false
    if (body is IrSyntheticBody) return false // Not sure what this IS, but it has no statements
    if (body.statements.size != 1) return false
    val statement = body.statements[0]
    if (statement !is IrReturn) return false
    val value = statement.value
    
    if (value is IrGetValue) return true
    
    return false
}

private fun isSingleStatementReturnPure(function: IrFunction): Boolean {
    val body = function.body ?: return false
    if (body is IrSyntheticBody) return false // Not sure what this IS, but it has no statements
    if (body.statements.size != 1) return false
    val statement = body.statements[0]
    if (statement !is IrReturn) return false
    
    val value = statement.value
    if (value is IrConst<*>) return true
    if (value is IrGetValue && !value.symbol.owner.let { it is IrVariable && it.isVar }) return true
    
    return false
}

enum class FunctionColoring{
    None,
    Readonly,
    Pure
}

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

/** Warns every time a var is set a value, or an unpure function is called.
 * Vars that are created within the function are OK to set */
class CheckFunctionColoringVisitor(
    private val function: IrFunction,
    private val declaredFunctionColoring: FunctionColoring,
    private val messageCollector: MessageCollector,
    private val wellKnownPureClassesFromUser: Set<String>,
    private val wellKnownPureFunctionsFromUser: Set<String>,
    private val wellKnownReadonlyFunctionsFromUser: Set<String>,
    ) : IrElementVisitor<Unit, Unit> { // Returns whether this is an acceptable X function
    var isReadonly = true
    var isPure = true
    
    fun actualFunctionColoring(): FunctionColoring {
        return when {
            isPure -> FunctionColoring.Pure
            isReadonly -> FunctionColoring.Readonly
            else -> FunctionColoring.None
        }
    }

    // Iterate over IR tree and warn on each var set where the var is not created within this function
    override fun visitSetValue(expression: IrSetValue, data: Unit) {
        // Not sure if we can assume owner is set at this point :think:
        val varValueDeclaration: IrValueDeclaration = expression.symbol.owner
        
        // If the variable is created in this function that's ok
        if (varValueDeclaration is IrVariable && varValueDeclaration.isVar && varValueDeclaration.parent != function) {
            isReadonly = false
            isPure = false

            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Function \"${function.name}\" is marked as $declaredFunctionColoring but sets variable \"${varValueDeclaration.name}\"",
                location = getLocationForExpression(function, expression)
            )
        }
        super.visitSetValue(expression, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit) {
        val varValueDeclaration: IrValueDeclaration = expression.symbol.owner
        
        // If the variable is created in this function that's ok
        if (varValueDeclaration is IrVariable && varValueDeclaration.isVar && varValueDeclaration.parent != function) {
            isPure = false

            if (declaredFunctionColoring == FunctionColoring.Pure) {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Function \"${function.name}\" is marked as $declaredFunctionColoring but gets variable \"${varValueDeclaration.name}\"",
                    location = getLocationForExpression(function, expression) 
                )
            }
        }
        super.visitGetValue(expression, data)
    }
    
    override fun visitCall(expression: IrCall, data: Unit) {
        // Only accept calls to functions marked as pure or readonly
        val calledFunction = expression.symbol.owner
        
        fun callerIsDeclaredInOurFunction() = expression.dispatchReceiver is IrGetValue &&
                (expression.dispatchReceiver as IrGetValue).symbol.owner.parent == function
        
        val calledFunctionColoring =  when {
            isMarkedAsPure(calledFunction, wellKnownPureClassesFromUser, wellKnownPureFunctionsFromUser) 
                    || (classMatches(calledFunction, wellKnownInternalStateClasses) && callerIsDeclaredInOurFunction())
                -> FunctionColoring.Pure
            isReadonly(calledFunction, wellKnownReadonlyFunctionsFromUser) -> FunctionColoring.Readonly
            else -> FunctionColoring.None
        }
        
        
        if (calledFunctionColoring < FunctionColoring.Pure) isPure = false
        if (calledFunctionColoring < FunctionColoring.Readonly) isReadonly = false
        
        if (declaredFunctionColoring > calledFunctionColoring) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Function \"${function.name}\" is marked as $declaredFunctionColoring " +
                        "but calls non-$declaredFunctionColoring function \"${expression.symbol.owner.fqNameForIrSerialization}\"",
                location = getLocationForExpression(function, expression)
            )
        }
        
        super.visitCall(expression, data) 
    }

    override fun visitElement(element: IrElement, data: Unit) {
        element.acceptChildren(this, data)
    }
}


internal class PurityElementTransformer(
    private val pluginContext: IrPluginContext,
    private val debugLogger: DebugLogger,
    private val wellKnownPureClassesFromUser: Set<String>,
    private val wellKnownPureFunctionsFromUser: Set<String>,
    private val wellKnownReadonlyFunctionsFromUser: Set<String>
) : IrElementTransformerVoidWithContext() {
    
    // These are created behind the scenes for every class, don't warn for them
    val autogeneratedFunctions = setOf(
        "equals",
        "hashCode",
        "toString"
    )
    val enumAutogeneratedFunctions = setOf(
        "values",
        "valueOf",
        "compareTo",
        "clone"
    )
    
    val componentRegex = "component\\d+".toRegex()
    
    fun isAutogeneratedFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return autogeneratedFunctions.contains(name) 
                || name.startsWith('<') // auto-generated functions like <init>, <get-name>, <set-name>
                || function.parentEnumClassOrNull != null && (name in enumAutogeneratedFunctions) // Enum values function
                || function.parentClassOrNull?.isData == true && componentRegex.matches(name) // componentN functions for data classes
                || function.parentClassOrNull?.isData == true && name == "copy" // copy function for data classes

    }
    
    fun isSuppressed(declaration: IrSimpleFunction): Boolean {
        val suppressAnnotation = declaration.annotations.findAnnotation(FqName("kotlin.Suppress"))
            ?: return false
        
        // getAnnotationArgumentValue does not work for varargs, so we find the vararg
        //.flatmap{} instead of .valueArguments[0] because Suppress can be called with zero parameters also -_-
        @Suppress("UNCHECKED_CAST")
        val suppressParameters: List<String> = suppressAnnotation.valueArguments.flatMap { (it as IrVarargImpl).elements }
            .mapNotNull{it as? IrConst<String>}.map { it.value }

        return suppressParameters.contains("yairm210.purity")
    }
    
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (isAutogeneratedFunction(declaration)) return super.visitSimpleFunction(declaration)
        
        // Skip interface/abstract functions that are not implemented
        if (declaration.body == null) return super.visitSimpleFunction(declaration)

        if (isSuppressed(declaration)) return super.visitSimpleFunction(declaration)

        val functionDeclaredColoring = when {
            isMarkedAsPure(declaration, wellKnownPureClassesFromUser, wellKnownPureFunctionsFromUser) -> FunctionColoring.Pure
            isReadonly(declaration, wellKnownReadonlyFunctionsFromUser) -> FunctionColoring.Readonly
            else -> FunctionColoring.None
        }
        val messageCollector = if (functionDeclaredColoring == FunctionColoring.None) MessageCollector.NONE 
        else debugLogger.messageCollector
        
        val visitor = CheckFunctionColoringVisitor(declaration, functionDeclaredColoring, messageCollector,
            wellKnownPureClassesFromUser,
            wellKnownPureFunctionsFromUser,
            wellKnownReadonlyFunctionsFromUser)
        declaration.accept(visitor, Unit)
        
        val actualColoring = visitor.actualFunctionColoring()
        if (functionDeclaredColoring != actualColoring){
            
            if (functionDeclaredColoring < actualColoring && 
                (isAutogeneratedFunction(declaration) // Don't warn for unmarked autogenerated functions - they are not under the user's control
                        || declaration.overriddenSymbols.any() // this is an override of another function
                        || declaration.parentClassOrNull?.isInterface == true
                        )
                ) {
                return super.visitSimpleFunction(declaration)
            }
            
            // if equal, no message; If less that declared, we already warn for each individual violation
            if (functionDeclaredColoring < actualColoring) {
                val message = when (actualColoring) {
                    FunctionColoring.Pure -> "Function \"${declaration.name}\" can be marked with @Contract(pure = true) to indicate it is pure"
                    FunctionColoring.Readonly -> "Function \"${declaration.name}\" can be marked with @Contract(\"readonly\") to indicate it is readonly"
                    else -> throw Exception("Unexpected function coloring: $actualColoring")
                }

                debugLogger.messageCollector.report(CompilerMessageSeverity.WARNING, message,
                    location = getLocationForExpression(declaration, declaration))
            }
        }
        
        return super.visitSimpleFunction(declaration)
    }

}