package com.huanli233.hibari.compiler.transformer

import com.huanli233.hibari.compiler.logging.error
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.exp

class HibariIrGenerationExtension(val messageCollector: MessageCollector): IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val runTunerCalls = mutableListOf<IrCall>()
        val tunerParams = mutableMapOf<IrSymbol, IrValueParameter>()
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
                            property.parent.let { it is IrFile && it.packageFqName.asString() == "com.huanli233.hibari.runtime" }
                        ) {
                            (currentFunction?.irElement as? IrFunction)?.let { irFunction ->
                                tunerParams[irFunction.symbol]?.let {
                                    return DeclarationIrBuilder(pluginContext, expression.symbol).irGet(it)
                                }
                            }
                        }
                    }

                    if (expression in runTunerCalls) {
                        val transformedExpression = super.visitCall(expression)
                        val call = transformedExpression as IrCall
                        val lambda = call.getValueArgument(0)
                            ?: error("runTunable requires a lambda argument")

                        val type = lambda.type

                        val invokeSymbol = type.getClass()?.functions
                            ?.first { it.name == OperatorNameConventions.INVOKE }?.symbol ?: error(
                            "Expected lambda to have invoke method"
                        )

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
        moduleFragment.acceptChildrenVoid(
            object : IrVisitorVoid() {
                override fun visitFile(declaration: IrFile) {
                    super.visitFile(declaration)
                    if (declaration.name == "Tunables.kt") {
//                        messageCollector.error(declaration.dump())
                    }
                }
            }
        )
    }
}