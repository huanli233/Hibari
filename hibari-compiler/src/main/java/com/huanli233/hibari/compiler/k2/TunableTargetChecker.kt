package com.huanli233.hibari.compiler.k2

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

internal sealed class FirInferenceNode(val element: FirElement) {
    open val kind: NodeKind get() = NodeKind.Expression
    abstract val type: InferenceNodeType?
    open val referenceContainer: FirInferenceNode? get() = null
    open val parameterIndex: Int get() = -1
    override fun hashCode(): Int = 31 * element.hashCode()
    override fun equals(other: Any?): Boolean = other is FirInferenceNode && other.element == element
}

private open class FirElementInferenceNode(element: FirElement) : FirInferenceNode(element) {
    override val type: InferenceNodeType? get() = null
}

private class FirCallableElementInferenceNode(val callable: FirCallableSymbol<*>, element: FirElement) : FirElementInferenceNode(element) {
    override val type: InferenceNodeType = InferenceCallableType(callable)
    override fun toString(): String = "${callable.name.toString()}()@${element.source?.startOffset}"
}

private class FirFunctionInferenceNode(val function: FirFunction) : FirInferenceNode(function) {
    override val kind get() = NodeKind.Function
    override val type = InferenceCallableType(function.symbol)
    override fun toString(): String = function.symbol.name.toString()
}

private class FirLambdaInferenceNode(val lambda: FirAnonymousFunctionExpression): FirElementInferenceNode(lambda) {
    override val kind: NodeKind get() = NodeKind.Lambda
    override val type = InferenceCallableType(lambda.anonymousFunction.symbol)
    override fun toString() = "<lambda:${lambda.source?.startOffset}>"
}

private class FirSamInferenceNode(val sam: FirSamConversionExpression): FirElementInferenceNode(sam) {
    override val kind: NodeKind get() = NodeKind.Lambda
    override val type: InferenceNodeType? =
        (sam.expression as? FirAnonymousFunctionExpression)?.let {
            InferenceCallableType(it.anonymousFunction.symbol)
        }

    override fun toString(): String = "<sam:${sam.source?.startOffset}>"
}

private class FirParameterReferenceNode(
    element: FirElement,
    override val parameterIndex: Int,
    override val referenceContainer: FirInferenceNode
) : FirElementInferenceNode(element) {
    override val kind: NodeKind get() = NodeKind.ParameterReference
    override fun toString(): String = "param:$parameterIndex"
}

private fun callableInferenceNodeOf(expression: FirElement, callable: FirCallableSymbol<*>, context: CheckerContext) =
    parameterInferenceNodeOrNull(expression, context) ?: (expression as? FirAnonymousFunction)?.let {
        context.session.tunableTargetSessionStorage.getLambdaExpression(expression)
    }?.let {
        inferenceNodeOf(it, context)
    } ?: FirCallableElementInferenceNode(callable, expression)

@OptIn(SymbolInternals::class)
private fun parameterInferenceNodeOrNull(expression: FirElement, context: CheckerContext): FirInferenceNode? {
    if (expression is FirFunctionCall) {
        val receiver = expression.explicitReceiver as? FirQualifiedAccessExpression ?: return null
        val parameterSymbol = receiver.toResolvedCallableSymbol() as? FirValueParameterSymbol ?: return null
        val function = parameterSymbol.containingDeclarationSymbol as? FirFunctionSymbol<*> ?: return null
        val index = function.valueParameterSymbols.filter { it.isTunable(context) }.indexOf(parameterSymbol)
        if (index >= 0) {
            return FirParameterReferenceNode(expression, index, inferenceNodeOf(function.fir, context))
        }
    }
    return null
}

private fun inferenceNodeOf(
    element: FirElement,
    context: CheckerContext
): FirInferenceNode {
    return context.session.tunableTargetSessionStorage.nodeCache.getValue(element, context)
}

internal sealed class InferenceNodeType {
    context(context: CheckerContext)
    abstract fun toScheme(): Scheme
    abstract fun isTypeFor(callable: FirCallableSymbol<*>): Boolean
}

private class InferenceCallableType(val callable: FirCallableSymbol<*>) : InferenceNodeType() {
    context(context: CheckerContext)
    override fun toScheme(): Scheme = callable.toScheme()
    override fun isTypeFor(callable: FirCallableSymbol<*>) = this.callable.callableId == callable.callableId
    override fun hashCode(): Int = 31 * callable.callableId.hashCode()
    override fun equals(other: Any?): Boolean =
        other is InferenceCallableType && other.callable.callableId == callable.callableId
}

context(context: CheckerContext)
fun FirCallableSymbol<*>.toScheme(): Scheme =
    with(context.session) {
        Scheme(
            target = schemeItem(),
            parameters = parameters().map { it.toScheme() }
        )
    }

context(context: CheckerContext)
fun FirCallableSymbol<*>.parameters(): List<FirValueParameterSymbol> =
    (this as? FirFunctionSymbol<*>)?.let {
        valueParameterSymbols.filter { it.isTunable(context) }
    } ?: emptyList()

context(session: FirSession)
fun FirCallableSymbol<*>.schemeItem(): Item {
    return Open(-1, isUnspecified = true)
}

context(session: FirSession)
fun FirBasedSymbol<*>.annotationArgument(classId: ClassId, argumentName: Name) =
    getAnnotationByClassId(classId, session)?.argument(argumentName)

context(session: FirSession)
fun FirAnnotationContainer.annotationArgument(classId: ClassId, argumentName: Name) =
    getAnnotationByClassId(classId, session)?.argument(argumentName)

fun FirAnnotation.argument(name: Name): Any? = argumentMapping.mapping[name]?.let {
    if ((it.resolvedType.isString || it.resolvedType.isPrimitive) && it is FirLiteralExpression)
        it.value
    else null
}

object TunableTargetChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()
            ?: return
        if (calleeFunction.isTunable(context.session)) {
            updateParents(context)
            val infer = FirApplierInferencer(context, reporter)
            val call = inferenceNodeOf(expression, context)
            val target = callableInferenceNodeOf(expression, calleeFunction, context)
            val parameters = with(context) {
                calleeFunction.parameters()
            }
            val argumentsMapping = expression.resolvedArgumentMapping
            val arguments = parameters.mapNotNull { parameter ->
                argumentsMapping?.firstNotNullOfOrNull { entry ->
                    if (entry.value == parameter.fir)
                        inferenceNodeOf(entry.key, context)
                    else null
                }
            }
            infer.visitCall(call, target, arguments)
        }
    }
}

internal class FirApplierInferencer(
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
) {
    private val session = context.session

    private val infer = ApplierInferencer(
        typeAdapter = object : TypeAdapter<InferenceNodeType> {
            override fun declaredSchemaOf(type: InferenceNodeType): Scheme = with(context) { type.toScheme() }
            override fun currentInferredSchemeOf(type: InferenceNodeType): Scheme? = null
            override fun updatedInferredScheme(type: InferenceNodeType, scheme: Scheme) {}
        },
        nodeAdapter = object : NodeAdapter<InferenceNodeType, FirInferenceNode> {
            override fun containerOf(node: FirInferenceNode): FirInferenceNode {
                with(context.session) {
                    var current = node.element.parent
                    while (current != null) {
                        when (current) {
                            is FirFunction -> return inferenceNodeOf(current, context)
                        }
                        current = current.parent
                    }
                    return node
                }
            }

            override fun kindOf(node: FirInferenceNode): NodeKind = node.kind

            override fun schemeParameterIndexOf(node: FirInferenceNode, container: FirInferenceNode): Int = node.parameterIndex

            override fun typeOf(node: FirInferenceNode): InferenceNodeType? = node.type

            override fun referencedContainerOf(node: FirInferenceNode): FirInferenceNode? = node.referenceContainer
        },
        errorReporter = object : ErrorReporter<FirInferenceNode> {
            private fun descriptionFrom(token: String): String =
                with(session) {
                    token
                }

            override fun reportCallError(node: FirInferenceNode, expected: String, received: String) {
                if (expected != received) {
                    val expectedDescription = descriptionFrom(expected)
                    val receivedDescription = descriptionFrom(received)
                    with(context) {
                        reporter.reportOn(
                            source = node.element.source,
                            factory = HibariErrors.HIBARI_APPLIER_CALL_MISMATCH,
                            a = expectedDescription,
                            b = receivedDescription,
                            context
                        )
                    }
                }
            }

            override fun reportParameterError(node: FirInferenceNode, index: Int, expected: String, received: String) {
                with(context) {
                    reporter.reportOn(
                        source = node.element.source,
                        factory = HibariErrors.HIBARI_APPLIER_PARAMETER_MISMATCH,
                        a = expected,
                        b = received,
                        context
                    )
                }
            }

            override fun log(node: FirInferenceNode?, message: String) {
            }
        },
        lazySchemeStorage = object : LazySchemeStorage<FirInferenceNode> {
            override fun getLazyScheme(node: FirInferenceNode): LazyScheme? =
                session.tunableTargetSessionStorage.getLazyScheme(node.element)

            override fun storeLazyScheme(node: FirInferenceNode, value: LazyScheme) {
                session.tunableTargetSessionStorage.storeLazyScheme(node.element, value)
            }
        }
    )

    fun visitCall(
        call: FirInferenceNode,
        target: FirInferenceNode,
        arguments: List<FirInferenceNode>
    ) {
        infer.visitCall(call, target, arguments)
    }
}

private fun updateParents(context: CheckerContext) {
    with(context.session) {
        val containingElements = context.containingElements
        for (i in (1..<containingElements.size).reversed()) {
            val element = containingElements[i]
            if (element.parent != null) break
            val parent = containingElements[i - 1]
            tunableTargetSessionStorage.storeParent(element, parent)
        }
    }
}

context(session: FirSession)
private val FirElement.parent: FirElement? get() = session.tunableTargetSessionStorage.getParent(this)

internal class TunableTargetSessionStorage(session: FirSession) : FirExtensionSessionComponent(session) {
    private val schemes = ConcurrentHashMap<FirElement, LazyScheme>()
    private val parent = ConcurrentHashMap<FirElement, FirElement>()
    private val lambdaToExpression = ConcurrentHashMap<FirAnonymousFunction, FirElement>()
    val nodeCache = session.firCachesFactory.createCache<FirElement, FirInferenceNode, CheckerContext> { element, context ->
        inferenceNodeOf(element, context)
    }

    fun getLazyScheme(element: FirElement): LazyScheme? = schemes[element]
    fun storeLazyScheme(element: FirElement, value: LazyScheme) {
        schemes[element] = value
    }

    fun getParent(element: FirElement): FirElement? = parent[element]
    fun storeParent(element: FirElement, parent: FirElement) {
        this.parent[element] = parent
    }

    fun getLambdaExpression(function: FirAnonymousFunction): FirElement? {
        return lambdaToExpression[function]
    }

    private fun inferenceNodeOf(element: FirElement, context: CheckerContext): FirInferenceNode =
        when (element) {
            is FirAnonymousFunctionExpression -> run {
                lambdaToExpression[element.anonymousFunction] = element
                FirLambdaInferenceNode(element)
            }
            is FirSamConversionExpression -> run {
                (element.expression as? FirAnonymousFunctionExpression)?.let {
                    lambdaToExpression[it.anonymousFunction] = element
                }
                FirSamInferenceNode(element)
            }
            is FirAnonymousFunction -> callableInferenceNodeOf(element, element.symbol, context)
            is FirFunction -> FirFunctionInferenceNode(element)
            else -> parameterInferenceNodeOrNull(element, context) ?: FirElementInferenceNode(element)
        }
}

private val FirSession.tunableTargetSessionStorage: TunableTargetSessionStorage by FirSession.sessionComponentAccessor()