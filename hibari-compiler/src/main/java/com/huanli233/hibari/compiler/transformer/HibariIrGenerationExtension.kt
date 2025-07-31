package com.huanli233.hibari.compiler.transformer

import com.huanli233.hibari.compiler.logging.error
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.copyAnnotationsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.UUID
import kotlin.collections.plus
import kotlin.math.exp

class HibariIrGenerationExtension(val messageCollector: MessageCollector): IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val runTunerCalls = mutableListOf<IrCall>()
        val tunerParams = mutableMapOf<IrSymbol, IrValueParameter>()
        moduleFragment.transformChildrenVoid(
            object : IrElementTransformerVoid() {
                val tunableAnnotation: IrClassSymbol = pluginContext.referenceClass(TUNABLE_ANNOTATION_ID)
                    ?: error("Cannot find annotation class ${TUNABLE_ANNOTATION_ID.asSingleFqName()}")

                private fun createTunableAnnotation() =
                    IrConstructorCallImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        type = tunableAnnotation.owner.defaultType,
                        symbol = tunableAnnotation.owner.primaryConstructor!!.symbol,
                        typeArgumentsCount = 0,
                        constructorTypeArgumentsCount = 0
                    )

                private val targetPropertyFqn = "com.huanli233.hibari.ui.uniqueKey"

                private fun generateUUIDConstant(original: IrCall): IrConstImpl {
                    val uuid = UUID.randomUUID().toString()

                    return IrConstImpl.string(
                        startOffset = original.startOffset,
                        endOffset = original.endOffset,
                        type = pluginContext.irBuiltIns.stringType,
                        value = uuid
                    )
                }

                override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
                    (declaration.defaultValue?.expression as? IrFunctionExpression)?.let { argument ->
                        if (declaration.type.isTunable()) {
                            if (!argument.function.isTunable()) {
                                argument.function.annotations += createTunableAnnotation()
                            }
                        }
                    }
                    return super.visitValueParameter(declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val ownerFn = expression.symbol.owner
                    ownerFn.valueParameters.forEach { parameter ->
                        if (parameter.type.isTunable()) {
                            val argument = expression.getValueArgument(parameter.index)
                            if (argument is IrFunctionExpression) {
                                if (!argument.function.isTunable()) {
                                    argument.function.annotations += createTunableAnnotation()
                                }
                            }
                        }
                    }
                    val function = expression.symbol.owner
                    if (function.isGetter && function.correspondingPropertySymbol?.owner?.fqNameWhenAvailable?.asString() == targetPropertyFqn) {
                        return generateUUIDConstant(expression)
                    }
                    return super.visitCall(expression)
                }

                override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                    val ownerFn = expression.symbol.owner
                    ownerFn.valueParameters.forEach { parameter ->
                        if (parameter.type.isTunable()) {
                            val argument = expression.getValueArgument(parameter.index)
                            if (argument is IrFunctionExpression) {
                                if (!argument.function.isTunable()) {
                                    argument.function.annotations += createTunableAnnotation()
                                }
                            }
                        }
                    }
                    return super.visitFunctionReference(expression)
                }

                override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                    val ownerFn = expression.symbol.owner
                    ownerFn.valueParameters.forEach { parameter ->
                        if (parameter.type.isTunable()) {
                            val argument = expression.getValueArgument(parameter.index)
                            if (argument is IrFunctionExpression) {
                                if (!argument.function.isTunable()) {
                                    argument.function.annotations += createTunableAnnotation()
                                }
                            }
                        }
                    }
                    return super.visitConstructorCall(expression)
                }

            }
        )
//        moduleFragment.acceptChildrenVoid(
//            object : IrVisitorVoid() {
//                override fun visitFile(declaration: IrFile) {
//                    super.visitFile(declaration)
//                    if (declaration.name == "Tuner.kt") {
//                        messageCollector.error(declaration.dump())
//                    }
//                }
//            }
//        )
        moduleFragment.transformChildrenVoid(TunerParamTransformer(pluginContext, messageCollector, runTunerCalls, tunerParams))
        moduleFragment.transformChildrenVoid(TunableTypeTransformer(pluginContext, messageCollector,
            TunableTypeRemapper(pluginContext, tunerType = pluginContext.referenceClass(TUNER_CLASS_ID)?.defaultType ?: error("Cannot find $TUNER_CLASS_ID"))))
        moduleFragment.patchDeclarationParents()
        moduleFragment.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {

                override fun visitCall(expression: IrCall): IrExpression {
                    val owner = expression.symbol.owner
                    if (owner.isGetter) {
                        val property = owner.correspondingPropertySymbol?.owner
                        if (property != null &&
                            property.name.asString() == "currentTuner" &&
                            property.symbol == pluginContext.referenceProperties(CallableId(FqName("com.huanli233.hibari.runtime"), null,
                                Name.identifier("currentTuner")
                            )).singleOrNull()
                        ) {
                            return expression.getValueArgument(0) ?: error("currentTuner should have a value argument")
                        }
                    }

                    if (expression in runTunerCalls) {
                        val transformedExpression = super.visitCall(expression)
                        val call = transformedExpression as IrCall
                        val lambda = call.getValueArgument(0)
                            ?: error("runTunable requires a lambda argument")

                        val type = lambda.type

                        val invokeSymbol = type.getClass()?.functions
                            ?.firstOrNull { it.name == OperatorNameConventions.INVOKE }?.symbol ?: let {
                            error(
                                "Expected lambda to have invoke method"
                            )
                        }

                        return IrCallImpl.fromSymbolOwner(
                            startOffset = call.startOffset,
                            endOffset = call.endOffset,
                            symbol = invokeSymbol
                        ).apply {
                            dispatchReceiver = lambda
                            putValueArgument(0, call.dispatchReceiver)
                        }
                    }
                    return super.visitCall(expression)
                }
            }
        )
    }
}