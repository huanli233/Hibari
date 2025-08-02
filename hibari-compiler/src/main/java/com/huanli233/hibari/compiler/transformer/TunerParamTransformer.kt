@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.huanli233.hibari.compiler.transformer

import com.huanli233.hibari.compiler.logging.error
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyAnnotationsFrom
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isPublishedApi
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

class TunerParamTransformer(
    pluginContext: IrPluginContext,
    messageCollector: MessageCollector,
    private val runTunerCalls: MutableList<IrCall>,
    private val tunerParams: MutableMap<IrSymbol, IrValueParameter>,
) : AbstractHibariLowering(pluginContext, messageCollector) {

    private var inlineLambdaInfo = HibariInlineLambdaLocator(context)
    private val functionsToSuppressGroupIn = mutableSetOf<IrSymbol>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        inlineLambdaInfo.scan(declaration)
        return super.visitModuleFragment(declaration)
    }

    private val transformedFunctions: MutableMap<IrSimpleFunction, IrSimpleFunction> =
        mutableMapOf()

    private val transformedFunctionSet = mutableSetOf<IrSimpleFunction>()

    private val tunerType = tunerClass.defaultType.replaceArgumentsWithStarProjections()

    private var currentParent: IrDeclarationParent? = null

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        val parent = currentParent
        if (declaration is IrDeclarationParent) {
            currentParent = declaration
        }
        return super.visitDeclaration(declaration).also {
            currentParent = parent
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
        super.visitSimpleFunction(declaration.withTunerParamIfNeeded())

    override fun visitLocalDelegatedPropertyReference(
        expression: IrLocalDelegatedPropertyReference,
    ): IrExpression {
        expression.getter = expression.getter.owner.withTunerParamIfNeeded().symbol
        expression.setter = expression.setter?.run { owner.withTunerParamIfNeeded().symbol }
        return super.visitLocalDelegatedPropertyReference(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.getter = expression.getter?.run { owner.withTunerParamIfNeeded().symbol }
        expression.setter = expression.setter?.run { owner.withTunerParamIfNeeded().symbol }
        return super.visitPropertyReference(expression)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) {
            if (inlineLambdaInfo.isInlineFunctionExpression(expression)) {
                return super.visitBlock(expression)
            }
            val functionRef =
                when (val last = expression.statements.lastOrNull()) {
                    is IrFunctionReference -> last
                    is IrTypeOperatorCall -> {
                        last.argument as? IrFunctionReference
                            ?: return super.visitBlock(expression)
                    }
                    else -> error("Unexpected adapted function reference shape: ${expression.dump()}")
                }
            if (!functionRef.type.isKTunableFunction() && !functionRef.type.isSyntheticTunableFunction()) {
                return super.visitBlock(expression)
            }

            val fn = functionRef.symbol.owner as? IrSimpleFunction
                ?: return super.visitBlock(expression)

            // Adapter functions are never restartable, but the original function might be.
            val adapterCall = fn.findCallInBody() ?: error("Expected a call in ${fn.dump()}")
            val originalFn = adapterCall.symbol.owner
            return adaptComposableReference(functionRef, originalFn, useAdaptedOrigin = false)
        }
        return super.visitBlock(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {

        if (!expression.type.isKTunableFunction() && !expression.type.isSyntheticTunableFunction()) {
            return super.visitFunctionReference(expression)
        }

        val fn = expression.symbol.owner as? IrSimpleFunction
            ?: return super.visitFunctionReference(expression)

        if (fn.origin != IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE &&
            !inlineLambdaInfo.isInlineFunctionExpression(expression)
        ) {
            // Non-restartable functions may not contain a group and should be wrapped with a separate
            // adapter. This is different from Kotlin's adapted function reference since it is treated
            // as a regular local function and is not inlined into AdaptedFunctionReference.
            // This might mess with the reflection that tries to find a containing class, but the name
            // will be preserved. This is fine, since AdaptedFunctionReference does not support reflection
            // either.
            return adaptComposableReference(expression, fn, useAdaptedOrigin = false)
        } else {
            return transformComposableFunctionReference(expression, fn)
        }
    }

    private fun IrFunction.findCallInBody(): IrCall? {
        var call: IrCall? = null
        body?.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (call == null) {
                    call = expression
                }
                return
            }
        })
        return call
    }

    private fun transformComposableFunctionReference(
        expression: IrFunctionReference,
        fn: IrSimpleFunction
    ): IrExpression {
        val type = expression.type as IrSimpleType
        val arity = type.arguments.size + /* tuner */ 1

        val newType = IrSimpleTypeImpl(
            classifier = if (expression.type.isKTunableFunction()) {
                context.irBuiltIns.kFunctionN(arity).symbol
            } else {
                context.irBuiltIns.functionN(arity).symbol
            },
            hasQuestionMark = type.isNullable(),
            arguments = buildList {
                addAll(type.arguments.dropLast(1))
                add(tunerType)
                add(type.arguments.last())
            },
            annotations = type.annotations
        )

        // Transform receiver arguments
        expression.transformChildrenVoid()

        // Adapted function calls created by Kotlin compiler don't copy annotations from the original function
        if (fn.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE && !fn.isTunable()) {
            fn.annotations += createTunableAnnotation()
        }

        return IrFunctionReferenceImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = newType,
            symbol = fn.withTunerParamIfNeeded().symbol,
            typeArgumentsCount = expression.typeArguments.size,
            reflectionTarget = expression.reflectionTarget?.owner?.let {
                if (it is IrSimpleFunction) it.withTunerParamIfNeeded().symbol else it.symbol
            },
            origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE,
        ).apply {
            typeArguments.assignFrom(expression.typeArguments)
            arguments.assignFrom(expression.arguments)
            repeat(arity - expression.arguments.size) {
                arguments.add(null)
            }
        }
    }

    private fun adaptComposableReference(
        expression: IrFunctionReference,
        fn: IrSimpleFunction,
        useAdaptedOrigin: Boolean
    ): IrExpression {
        val adapter = irBlock(
            type = expression.type,
            origin = if (useAdaptedOrigin) IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE else null,
            statements = buildList {
                val localParent = currentParent ?: error("No parent found for ${expression.dump()}")
                val adapterFn = context.irFactory.buildFun {
                    origin =
                        if (useAdaptedOrigin) IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE else origin
                    name = fn.name
                    visibility = DescriptorVisibilities.LOCAL
                    modality = Modality.FINAL
                    returnType = fn.returnType
                }
                adapterFn.copyAnnotationsFrom(fn)

                if (!adapterFn.isTunable()) {
                    adapterFn.annotations += createTunableAnnotation()
                }

                adapterFn.copyParametersFrom(fn)
                require(
                    adapterFn.parameters.count {
                        it.kind == IrParameterKind.ExtensionReceiver ||
                                it.kind == IrParameterKind.DispatchReceiver
                    } <= 1
                ) {
                    "Function references are not allowed to have multiple receivers: ${expression.dump()}"
                }
                adapterFn.parameters = buildList {
                    val receiver = adapterFn.parameters.find {
                        it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver
                    }
                    if (receiver != null) {
                        // Match IR generated by the FIR2IR adapter codegen.
                        receiver.kind = IrParameterKind.ExtensionReceiver
                        receiver.name = Name.identifier("receiver")
                        add(receiver)
                    }

                    // The adapter function should have the same parameters as the KComposableFunction type.
                    // Receivers are processed separately and are not included in the parameter count.
                    val type = expression.type as IrSimpleType
                    var n = type.arguments.size - /* return type */ 1
                    adapterFn.parameters.forEach {
                        if (it.kind == IrParameterKind.Regular && n-- > 0) {
                            add(it)
                        }
                    }
                }

                adapterFn.body =
                    context.irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                        statements.add(
                            irReturn(
                                adapterFn.symbol,
                                irCall(fn.symbol).also { call ->
                                    fn.parameters.forEach {
                                        call.arguments[it.indexInParameters] = when (it.kind) {
                                            IrParameterKind.Context -> {
                                                // Should be unreachable (see CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION)
                                                error("Context parameters are not supported in function references")
                                            }

                                            IrParameterKind.DispatchReceiver,
                                            IrParameterKind.ExtensionReceiver -> {
                                                adapterFn.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }
                                            }

                                            IrParameterKind.Regular -> {
                                                adapterFn.parameters.getOrNull(it.indexInParameters)
                                            }
                                        }?.let(::irGet)
                                    }
                                }
                            )
                        )
                    }
                adapterFn.parent = localParent
                add(adapterFn)

                add(
                    IrFunctionReferenceImpl(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        type = expression.type,
                        symbol = adapterFn.symbol,
                        typeArgumentsCount = expression.typeArguments.size,
                        reflectionTarget = fn.symbol,
                        origin = if (useAdaptedOrigin) IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE else null
                    ).apply {
                        typeArguments.assignFrom(expression.typeArguments)
                        arguments.assignFrom(expression.arguments)
                    }
                )
            }
        )

        // Pass the adapted function reference to the transformer to handle the adapted function the same way as regular composables.
        return visitBlock(adapter)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        if (declaration.getter.isComposableDelegatedAccessor()) {
            declaration.getter.annotations += createTunableAnnotation()
        }

        if (declaration.setter?.isComposableDelegatedAccessor() == true) {
            declaration.setter!!.annotations += createTunableAnnotation()
        }

        return super.visitLocalDelegatedProperty(declaration)
    }

    private fun createTunableAnnotation() =
        IrConstructorCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = tunableAnnotation.owner.defaultType,
            symbol = tunableAnnotation.owner.primaryConstructor!!.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0
        )

    fun IrCall.withTunerParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val newFn = when {
            symbol.owner.isComposableDelegatedAccessor() -> {
                if (!symbol.owner.isTunable()) {
                    symbol.owner.annotations += createTunableAnnotation()
                }
                symbol.owner.withTunerParamIfNeeded()
            }

            isComposableLambdaInvoke() ->
                symbol.owner.lambdaInvokeWithTunerParam()

            symbol.owner.isTunable() ->
                symbol.owner.withTunerParamIfNeeded()
            // Not a tunable call
            else -> return this
        }

        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            newFn.symbol,
            typeArguments.size,
            origin,
            superQualifierSymbol
        ).also { newCall ->
            newCall.copyAttributes(this)
            newCall.copyTypeArgumentsFrom(this)

            val argumentsMissing = mutableListOf<Boolean>()
            arguments.forEachIndexed { i, arg ->
                val p = newFn.parameters[i]
                when (p.kind) {
                    IrParameterKind.DispatchReceiver,
                    IrParameterKind.ExtensionReceiver,
                    IrParameterKind.Context -> {
                        newCall.arguments[p.indexInParameters] = arg
                    }

                    IrParameterKind.Regular -> {
                        val hasDefault = newFn.hasDefaultForParam(i)
                        argumentsMissing.add(arg == null && hasDefault)
                        if (arg != null) {
                            newCall.arguments[p.indexInParameters] = arg
                        } else if (hasDefault) {
                            // None
                        }
                    }
                }
            }

            val valueParamCount = arguments.indices.count { i ->
                val p = newFn.parameters[i]
                p.kind == IrParameterKind.Regular
            }
            var argIndex = arguments.size
            newCall.arguments[argIndex++] = irGet(composerParam)
        }
    }

    private fun defaultArgumentFor(param: IrValueParameter): IrExpression? {
        return param.type.defaultValue().let {
            IrCompositeImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                IrStatementOrigin.DEFAULT_VALUE,
                listOf(it)
            )
        }
    }

    // TODO(lmr): There is an equivalent function in IrUtils, but we can't use it because it
    //  expects a JvmBackendContext. That implementation uses a special "unsafe coerce" builtin
    //  method, which is only available on the JVM backend. On the JS and Native backends we
    //  don't have access to that so instead we are just going to construct the inline class
    //  itself and hope that it gets lowered properly.
    private fun IrType.defaultValue(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression {
        val classSymbol = classOrNull
        if (this !is IrSimpleType || isMarkedNullable() || !isInlineClassType()) {
            return if (isMarkedNullable()) {
                IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            } else {
                IrConstImpl.defaultValueForType(startOffset, endOffset, this)
            }
        }

        if (context.platform.isJvm()) {
            val underlyingType = unboxInlineClass()
            return coerceInlineClasses(
                IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType),
                underlyingType,
                this
            )
        } else {
            val ctor = classSymbol!!.constructors.first { it.owner.isPrimary }
            val underlyingType = getInlineClassUnderlyingType(classSymbol.owner)

            // TODO(lmr): We should not be calling the constructor here, but this seems like a
            //  reasonable interim solution.
            return IrConstructorCallImpl(
                startOffset,
                endOffset,
                this,
                ctor,
                typeArgumentsCount = 0,
                constructorTypeArgumentsCount = 0,
                origin = null
            ).also {
                it.arguments[0] = underlyingType.defaultValue(startOffset, endOffset)
            }
        }
    }

    // Transform `@Composable fun foo(params): RetType` into `fun foo(params, $composer: Composer): RetType`
    private fun IrSimpleFunction.withTunerParamIfNeeded(): IrSimpleFunction {
        // don't transform functions that themselves were produced by this function. (ie, if we
        // call this with a function that has the synthetic composer parameter, we don't want to
        // transform it further).
        if (transformedFunctionSet.contains(this)) return this

        // if not a composable fn, nothing we need to do
        if (!this.isTunable()) {
            return this
        }

        // we don't bother transforming expect functions. They exist only for type resolution and
        // don't need to be transformed to have a composer parameter
        if (isExpect) return this

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: this.copyWithTunerParam()
    }

    private fun IrSimpleFunction.lambdaInvokeWithTunerParam(): IrSimpleFunction {
        val argCount = parameters.size
        val extraParams = 1
        val newFnClass =
            context.irBuiltIns.functionN(argCount + extraParams - /* dispatch receiver */ 1)
        val newInvoke = newFnClass.functions.first {
            it.name == OperatorNameConventions.INVOKE
        }
        return newInvoke
    }


    private fun IrSimpleFunction.hasDefaultForParam(index: Int): Boolean {
        // checking for default value isn't enough, you need to ensure that none of the overrides
        // have it as well...
        if (parameters[index].kind != IrParameterKind.Regular) return false
        if (parameters[index].defaultValue != null) return true

        return overriddenSymbols.any {
            // ComposableFunInterfaceLowering copies extension receiver as a value
            // parameter, which breaks indices for overrides. fun interface cannot
            // have parameters defaults, however, so we can skip here if mismatch is detected.
            it.owner.parameters.size == parameters.size &&
                    it.owner.hasDefaultForParam(index)
        }
    }

    internal inline fun <reified T : IrElement> T.deepCopyWithSymbolsAndMetadata(
        initialParent: IrDeclarationParent? = null,
        createTypeRemapper: (SymbolRemapper) -> TypeRemapper = ::DeepCopyTypeRemapper,
    ): T {
        val symbolRemapper = DeepCopySymbolRemapper()
        acceptVoid(symbolRemapper)
        val typeRemapper = createTypeRemapper(symbolRemapper)
        return (transform(
            DeepCopyPreservingMetadata(symbolRemapper, typeRemapper),
            null
        ) as T).patchDeclarationParents(initialParent)
    }

    private fun IrSimpleFunction.copyWithTunerParam(): IrSimpleFunction {
        assert(parameters.lastOrNull()?.name != Name.identifier(TUNER_PARAMETER_NAME)) {
            "Attempted to add composer param to $this, but it has already been added."
        }
        return deepCopyWithSymbolsAndMetadata(parent).also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            fn.metadata = oldFn.metadata

            // The overridden symbols might also be composable functions, so we want to make sure
            // and transform them as well
            fn.overriddenSymbols = oldFn.overriddenSymbols.map {
                it.owner.withTunerParamIfNeeded().symbol
            }

            val propertySymbol = oldFn.correspondingPropertySymbol
            if (propertySymbol != null) {
                fn.correspondingPropertySymbol = propertySymbol
                if (propertySymbol.owner.getter == oldFn) {
                    propertySymbol.owner.getter = fn
                }
                if (propertySymbol.owner.setter == oldFn) {
                    propertySymbol.owner.setter = fn
                }
            }

            val currentParams = fn.parameters.count { it.kind == IrParameterKind.Regular }
            val realParams = currentParams

            // $tuner
            val tunerParam = fn.addValueParameter {
                name = Name.identifier(TUNER_PARAMETER_NAME)
                type = tunerType.makeNullable()
                origin = IrDeclarationOrigin.DEFINED
                isAssignable = true
            }

            tunerParams[fn.symbol] = tunerParam
            val suppressGroupInjection = functionsToSuppressGroupIn.contains(this.symbol)

            // update parameter types so they are ready to accept the default values
            fn.parameters.forEach { param ->
                if (fn.hasDefaultForParam(param.indexInParameters)) {
                    param.type = param.type.defaultParameterType()
                }
            }

            inlineLambdaInfo.scan(fn)

            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false
                override fun visitFunction(declaration: IrFunction): IrStatement {
                    if (declaration is IrSimpleFunction && declaration.isTunable()) {
                        return declaration.withTunerParamIfNeeded()
                    }

                    val wasNested = isNestedScope
                    try {
                        isNestedScope = wasNested ||
                                !inlineLambdaInfo.isInlineLambda(declaration) ||
                                declaration.isTunable()
                        return super.visitFunction(declaration)
                    } finally {
                        isNestedScope = wasNested
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    expression.transformChildrenVoid(this)
                    val transformedCall = if (!isNestedScope) {
                        expression.withTunerParamIfNeeded(tunerParam)
                    } else {
                        expression
                    }

                    val emitNode = context.referenceFunctions(
                        CallableId(
                            packageName = FqName("com.huanli233.hibari.runtime"),
                            className = FqName("Tuner"),
                            callableName = Name.identifier("emitNode")
                        )
                    )
                    val wasTransformed = transformedCall !== expression
                    val isEmitNodeCall = expression.symbol in emitNode
                    val owner = expression.symbol.owner

                    val isCurrentTunerCall = if (owner.isGetter) {
                        val property = owner.correspondingPropertySymbol?.owner
                        property != null &&
                                property.name.asString() == "currentTuner" &&
                                property.symbol == context.referenceProperties(
                            CallableId(
                                FqName("com.huanli233.hibari.runtime"), null,
                                Name.identifier("currentTuner")
                            )
                        ).singleOrNull()
                    } else {
                        false
                    }

                    if (!suppressGroupInjection && !isCurrentTunerCall && fn.fqNameWhenAvailable != FqName("com.huanli233.hibari.runtime.key") && (wasTransformed || isEmitNodeCall)) {
                        return DeclarationIrBuilder(
                            context,
                            currentScope!!.scope.scopeOwnerSymbol
                        ).irBlock(
                            startOffset = expression.startOffset,
                            endOffset = expression.endOffset,
                            resultType = transformedCall.type
                        ) {
                            val key = keyGenerator.getAndIncrement()
                            val keyConst = IrConstImpl.int(
                                SYNTHETIC_OFFSET,
                                SYNTHETIC_OFFSET,
                                context.irBuiltIns.intType,
                                key
                            )

                            val startGroupCall = irCall(tunerStartGroupFunc).apply {
                                dispatchReceiver = irGet(tunerParam)
                                putValueArgument(0, keyConst)
                            }
                            +startGroupCall

                            val resultTmp = if (transformedCall.type.isUnit()) {
                                +transformedCall
                                null
                            } else {
                                irTemporary(transformedCall)
                            }

                            val endGroupCall = irCall(tunerEndGroupFunc).apply {
                                dispatchReceiver = irGet(tunerParam)
                                putValueArgument(
                                    0,
                                    IrConstImpl.int(
                                        SYNTHETIC_OFFSET,
                                        SYNTHETIC_OFFSET,
                                        context.irBuiltIns.intType,
                                        key
                                    )
                                )
                            }
                            +endGroupCall

                            if (resultTmp != null) {
                                +irGet(resultTmp)
                            }
                        }
                    }

                    return transformedCall
                }
            })
        }
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        if (expression.type.isTunable()) {
            val function = expression.function
            if (!function.isTunable()) {
                function.annotations += createTunableAnnotation()
            }
        }
        return super.visitFunctionExpression(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val ownerFn = expression.symbol.owner

        val isRunTunable = ownerFn.name.asString() == "runTunable" && ownerFn.valueParameters.getOrNull(0)?.type?.isTunable() == true &&
                ownerFn.parentAsClass.fqNameWhenAvailable?.asString() == "com.huanli233.hibari.runtime.Tuner"

        if (isRunTunable) {
            runTunerCalls.add(expression)
            val blockArgument = expression.getValueArgument(ownerFn.valueParameters.last().index)
            if (blockArgument is IrFunctionExpression) {
                functionsToSuppressGroupIn.add(blockArgument.function.symbol)
            }
        }

        return super.visitCall(expression)
    }


    private fun IrSimpleFunction.isPublicComposableFunction(): Boolean =
        isTunable() && (visibility.isPublicAPI || isPublishedApi())
}

internal open class DeepCopyPreservingMetadata(
    symbolRemapper: SymbolRemapper,
    typeRemapper: TypeRemapper,
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
    override fun visitFile(declaration: IrFile): IrFile =
        super.visitFile(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitClass(declaration: IrClass): IrClass =
        super.visitClass(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        super.visitConstructor(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        super.visitSimpleFunction(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitProperty(declaration: IrProperty): IrProperty =
        super.visitProperty(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitField(declaration: IrField): IrField =
        super.visitField(declaration).apply {
            metadata = declaration.metadata
        }

    override fun visitLocalDelegatedProperty(
        declaration: IrLocalDelegatedProperty,
    ): IrLocalDelegatedProperty =
        super.visitLocalDelegatedProperty(declaration).apply {
            metadata = declaration.metadata
        }
}