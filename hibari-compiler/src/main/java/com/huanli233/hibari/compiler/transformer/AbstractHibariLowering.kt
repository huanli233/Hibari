@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.huanli233.hibari.compiler.transformer

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.concurrent.atomic.AtomicInteger

val TUNABLE_ANNOTATION_ID = ClassId.fromString("com/huanli233/hibari/runtime/Tunable")
val DISALLOW_TUNABLE_CALLS_ANNOTATION_ID = ClassId.fromString("com/huanli233/hibari/runtime/DisallowTunableCalls")

val TUNER_CLASS_ID = ClassId.fromString("com/huanli233/hibari/runtime/Tuner")
val TUNER_START_GROUP_NAME = Name.identifier("startGroup")
val TUNER_END_GROUP_NAME = Name.identifier("endGroup")

const val TUNER_PARAMETER_NAME = "\$tuner"

@OptIn(UnsafeDuringIrConstructionAPI::class)
abstract class AbstractHibariLowering(
    val context: IrPluginContext,
    val messageCollector: MessageCollector
): IrElementTransformerVoidWithContext() {
    val tunableAnnotation: IrClassSymbol = context.referenceClass(TUNABLE_ANNOTATION_ID)
        ?: error("Cannot find annotation class ${TUNABLE_ANNOTATION_ID.asSingleFqName()}")
    val disallowAnnotation: IrClassSymbol = context.referenceClass(DISALLOW_TUNABLE_CALLS_ANNOTATION_ID)
        ?: error("Cannot find annotation class ${DISALLOW_TUNABLE_CALLS_ANNOTATION_ID.asSingleFqName()}")
    val tunerClass: IrClassSymbol = context.referenceClass(TUNER_CLASS_ID)
        ?: error("Cannot find Tuner class ${TUNER_CLASS_ID.asSingleFqName()}")

    val tunerStartGroupFunc: IrSimpleFunctionSymbol = tunerClass.functions
        .single { it.owner.name == TUNER_START_GROUP_NAME && it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.intType }
    val tunerEndGroupFunc: IrSimpleFunctionSymbol = tunerClass.functions
        .single { it.owner.name == TUNER_END_GROUP_NAME && it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.intType }

    companion object {
        val keyGenerator = AtomicInteger((1..(Int.MAX_VALUE / 2)).random())
    }
    val tunerParameters = mutableMapOf<IrSymbol, IrValueParameter>()

    protected val builtIns = context.irBuiltIns

    fun IrType.unboxInlineClass() = unboxType() ?: this

    fun IrType.defaultParameterType(): IrType {
        val type = this
        val constructorAccessible = !type.isPrimitiveType() &&
                type.classOrNull?.owner?.primaryConstructor != null
        return when {
            type.isPrimitiveType() -> type
            type.isInlineClassType() -> if (context.platform.isJvm() || constructorAccessible) {
                if (type.unboxInlineClass().isPrimitiveType()) {
                    type
                } else {
                    type.makeNullable()
                }
            } else {
                // k/js and k/native: private constructors of value classes can be not accessible.
                // Therefore it won't be possible to create a "fake" default argument for calls.
                // Making it nullable allows to pass null.
                type.makeNullable()
            }
            else -> type.makeNullable()
        }
    }

    fun IrType.replaceArgumentsWithStarProjections(): IrType =
        when (this) {
            is IrSimpleType -> IrSimpleTypeImpl(
                classifier,
                isMarkedNullable(),
                List(arguments.size) { IrStarProjectionImpl },
                annotations
            )

            else -> this
        }

    // NOTE(lmr): This implementation mimics the kotlin-provided unboxInlineClass method, except
    // this one makes sure to bind the symbol if it is unbound, so is a bit safer to use.
    fun IrType.unboxType(): IrType? {
        val klass = classOrNull?.owner ?: return null
        val representation = klass.inlineClassRepresentation ?: return null
        if (!isInlineClassType()) return null

        // TODO: Apply type substitutions
        val underlyingType = representation.underlyingType.unboxInlineClass()
        if (!isNullable()) return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }

    protected fun IrExpression.unboxValueIfInline(): IrExpression {
        if (type.isNullable()) return this
        val classSymbol = type.classOrNull ?: return this
        val klass = classSymbol.owner
        if (type.isInlineClassType()) {
            if (context.platform.isJvm()) {
                return coerceInlineClasses(
                    this,
                    type,
                    type.unboxInlineClass()
                ).unboxValueIfInline()
            } else {
                val primaryValueParameter = klass.primaryConstructor?.parameters?.singleOrNull { it.kind == IrParameterKind.Regular }
                val cantUnbox = primaryValueParameter == null || klass.properties.none {
                    it.name == primaryValueParameter.name && it.getter != null
                }
                if (cantUnbox) {
                    // LazyIr (external module) doesn't show a getter of a private property.
                    // So we can't unbox the value
                    return this
                }
                val fieldGetter = klass.getPropertyGetter(primaryValueParameter.name.identifier)
                    ?: error("Expected a getter")
                return irCall(
                    symbol = fieldGetter,
                    dispatchReceiver = this
                ).unboxValueIfInline()
            }
        }
        return this
    }

    fun IrCall.isInvoke(): Boolean {
        if (origin == IrStatementOrigin.INVOKE)
            return true
        val function = symbol.owner
        return function.name == OperatorNameConventions.INVOKE &&
                function.parentClassOrNull?.defaultType?.let {
                    it.isFunction() || it.isSyntheticTunableFunction()
                } ?: false
    }

    fun IrCall.isTunableCall(): Boolean {
        return symbol.owner.isTunable() || isComposableLambdaInvoke()
    }

    fun IrCall.isSyntheticTunableCall(): Boolean {
        return isTunableCall()
    }

    fun IrCall.isComposableLambdaInvoke(): Boolean {
        if (!isInvoke()) return false
        // [ComposerParamTransformer] replaces composable function types of the form
        // `@Composable Function1<T1, T2>` with ordinary functions with extra parameters, e.g.,
        // `Function3<T1, Composer, Int, T2>`. After this lowering runs we have to check the
        // `attributeOwnerId` to recover the original type.
        val receiver = dispatchReceiver?.let { it.attributeOwnerId as? IrExpression ?: it }
        return receiver?.type?.let {
            it.isTunable() || it.isSyntheticTunableFunction()
        } ?: false
    }

    // set the bit at a certain index
    private fun Int.withBit(index: Int, value: Boolean): Int {
        return if (value) {
            this or (1 shl index)
        } else {
            this and (1 shl index).inv()
        }
    }

    protected operator fun Int.get(index: Int): Boolean {
        return this and (1 shl index) != 0
    }

    // create a bitmask with the following bits
    protected fun bitMask(vararg values: Boolean): Int = values.foldIndexed(0) { i, mask, bit ->
        mask.withBit(i, bit)
    }

    protected fun irSet(variable: IrValueDeclaration, value: IrExpression): IrExpression {
        return IrSetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            variable.symbol,
            value = value,
            origin = null
        )
    }

    protected fun irCall(
        symbol: IrFunctionSymbol
    ): IrCallImpl =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol as IrSimpleFunctionSymbol,
        )

    protected fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        returnType: IrType = symbol.owner.returnType,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression,
    ): IrCallImpl {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            origin
        ).also { call ->
            var argIndex = 0
            symbol.owner.parameters.forEach {
                when (it.kind) {
                    IrParameterKind.DispatchReceiver -> {
                        call.arguments[it.indexInParameters] = dispatchReceiver
                    }
                    IrParameterKind.ExtensionReceiver -> {
                        call.arguments[it.indexInParameters] = extensionReceiver
                    }
                    IrParameterKind.Context,
                    IrParameterKind.Regular -> {
                        call.arguments[it.indexInParameters] = args[argIndex++]
                    }
                }
            }
        }
    }

    protected fun IrType.binaryOperator(name: Name, paramType: IrType): IrFunctionSymbol =
        context.symbols.getBinaryOperator(name, this, paramType)

    private fun binaryOperatorCall(
        lhs: IrExpression,
        rhs: IrExpression,
        name: Name,
        lhsType: IrType = lhs.type,
        rhsType: IrType = rhs.type
    ): IrCallImpl {
        val symbol = lhsType.binaryOperator(name, rhsType)
        return irCall(
            symbol = symbol,
            dispatchReceiver = lhs,
            args = arrayOf(rhs)
        )
    }

    protected fun irAnd(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.AND)
    }

    protected fun irShl(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.SHL, lhsType = int, rhsType = int)
    }

    protected fun irOr(lhs: IrExpression, rhs: IrExpression): IrExpression {
        if (rhs is IrConst && rhs.value == 0) return lhs
        if (lhs is IrConst && lhs.value == 0) return rhs
        val int = context.irBuiltIns.intType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.OR, lhsType = int, rhsType = int)
    }

    protected fun irBooleanOr(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val boolean = context.irBuiltIns.booleanType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.OR, lhsType = boolean, rhsType = boolean)
    }

    protected fun irOrOr(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.OROR,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = irConst(true)
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = rhs
                )
            )
        )
    }

    protected fun irAndAnd(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.ANDAND,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = rhs
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = irConst(false)
                )
            )
        )
    }

    protected fun irXor(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        return binaryOperatorCall(lhs, rhs, OperatorNameConventions.XOR, lhsType = int, rhsType = int)
    }

    protected fun irGreater(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.irBuiltIns.intType
        val gt = context.irBuiltIns.greaterFunByOperandType[int.classifierOrFail]
        return irCall(
            symbol = gt!!,
            origin = IrStatementOrigin.GT,
            args = arrayOf(lhs, rhs)
        )
    }

    protected fun irReturn(
        target: IrReturnTargetSymbol,
        value: IrExpression,
        type: IrType = value.type,
    ): IrExpression {
        return IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            target,
            value
        )
    }

    protected fun irReturnVar(
        target: IrReturnTargetSymbol,
        value: IrVariable,
    ): IrExpression {
        return IrReturnImpl(
            value.initializer?.startOffset ?: UNDEFINED_OFFSET,
            value.initializer?.endOffset ?: UNDEFINED_OFFSET,
            value.type,
            target,
            irGet(value)
        )
    }

    /** Compare [lhs] and [rhs] using structural equality (`==`). */
    protected fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            symbol = context.irBuiltIns.eqeqSymbol,
            args = arrayOf(lhs, rhs)
        )
    }

    protected fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            dispatchReceiver = value
        )
    }

    /** Compare [lhs] and [rhs] using structural inequality (`!=`). */
    protected fun irNotEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irNot(irEqual(lhs, rhs))
    }

//        context.irIntrinsics.symbols.intAnd
//        context.irIntrinsics.symbols.getBinaryOperator(name, lhs, rhs)
//        context.irBuiltIns.booleanNotSymbol
//        context.irBuiltIns.eqeqeqSymbol
//        context.irBuiltIns.eqeqSymbol
//        context.irBuiltIns.greaterFunByOperandType

    protected fun irConst(value: Int): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.intType,
        IrConstKind.Int,
        value
    )

    protected fun irConst(value: Long): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.longType,
        IrConstKind.Long,
        value
    )

    protected fun irConst(value: String): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.stringType,
        IrConstKind.String,
        value
    )

    protected fun irConst(value: Boolean) = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.booleanType,
        IrConstKind.Boolean,
        value
    )

    protected fun irNull() = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.anyNType,
        IrConstKind.Null,
        null
    )

    protected fun irForLoop(
        elementType: IrType,
        subject: IrExpression,
        loopBody: (IrValueDeclaration) -> IrExpression,
    ): IrStatement {
        val getIteratorFunction = subject.type.classOrNull!!.owner.functions
            .single { it.name.asString() == "iterator" }

        val iteratorSymbol = getIteratorFunction.returnType.classOrNull!!
        val iteratorType = if (iteratorSymbol.owner.typeParameters.isNotEmpty()) {
            iteratorSymbol.typeWith(elementType)
        } else {
            iteratorSymbol.defaultType
        }

        val nextSymbol = iteratorSymbol.owner.functions
            .single { it.name.asString() == "next" }
        val hasNextSymbol = iteratorSymbol.owner.functions
            .single { it.name.asString() == "hasNext" }

        val call = irCall(
            symbol = getIteratorFunction.symbol,
            origin = IrStatementOrigin.FOR_LOOP_ITERATOR,
            returnType = iteratorType,
            dispatchReceiver = subject
        )

        val iteratorVar = irTemporary(
            value = call,
            isVar = false,
            name = "tmp0_iterator",
            irType = iteratorType,
            origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
        )
        return irBlock(
            type = builtIns.unitType,
            origin = IrStatementOrigin.FOR_LOOP,
            statements = listOf(
                iteratorVar,
                IrWhileLoopImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    builtIns.unitType,
                    IrStatementOrigin.FOR_LOOP_INNER_WHILE
                ).apply {
                    val loopVar = irTemporary(
                        value = irCall(
                            symbol = nextSymbol.symbol,
                            origin = IrStatementOrigin.FOR_LOOP_NEXT,
                            returnType = elementType,
                            dispatchReceiver = irGet(iteratorVar)
                        ),
                        origin = IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                        isVar = false,
                        name = "value",
                        irType = elementType
                    )
                    condition = irCall(
                        symbol = hasNextSymbol.symbol,
                        origin = IrStatementOrigin.FOR_LOOP_HAS_NEXT,
                        dispatchReceiver = irGet(iteratorVar)
                    )
                    body = irBlock(
                        type = builtIns.unitType,
                        origin = IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                        statements = listOf(
                            loopVar,
                            loopBody(loopVar)
                        )
                    )
                }
            )
        )
    }

    protected fun irTemporary(
        value: IrExpression,
        name: String,
        irType: IrType = value.type,
        isVar: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    ): IrVariableImpl {
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            origin,
            IrVariableSymbolImpl(),
            Name.identifier(name),
            irType,
            isVar,
            isConst = false,
            isLateinit = false
        ).apply {
            initializer = value
        }
    }

    protected fun irGet(type: IrType, symbol: IrValueSymbol): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    protected fun irGet(variable: IrValueDeclaration): IrExpression {
        return irGet(variable.type, variable.symbol)
    }

    protected fun irGetField(field: IrField): IrGetField {
        return IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            field.symbol,
            field.type
        )
    }

    protected fun irIf(condition: IrExpression, body: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            origin = IrStatementOrigin.IF
        ).also {
            it.branches.add(
                IrBranchImpl(condition, body)
            )
        }
    }

    protected fun irIfThenElse(
        type: IrType = context.irBuiltIns.unitType,
        condition: IrExpression,
        thenPart: IrExpression,
        elsePart: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ) =
        IrWhenImpl(startOffset, endOffset, type, IrStatementOrigin.IF).apply {
            branches.add(
                IrBranchImpl(
                    startOffset,
                    endOffset,
                    condition,
                    thenPart
                )
            )
            branches.add(irElseBranch(elsePart, startOffset, endOffset))
        }

    protected fun irWhen(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        branches: List<IrBranch>,
    ) = IrWhenImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        origin,
        branches
    )

    protected fun irBranch(
        condition: IrExpression,
        result: IrExpression,
    ): IrBranch {
        return IrBranchImpl(condition, result)
    }

    protected fun irElseBranch(
        expression: IrExpression,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ) = IrElseBranchImpl(startOffset, endOffset, irConst(true), expression)

    protected fun irBlock(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        statements: List<IrStatement>,
    ): IrBlock {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            origin,
            statements
        )
    }

    protected fun irComposite(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>,
    ): IrExpression {
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        returnType: IrType,
        body: (IrSimpleFunction) -> Unit,
    ): IrExpression {
        val function = context.irFactory.buildFun {
            this.startOffset = SYNTHETIC_OFFSET
            this.endOffset = SYNTHETIC_OFFSET
            this.returnType = returnType
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
        }.also(body)

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = context.irBuiltIns.functionN(function.parameters.size).typeWith(
                function.parameters.map { it.type } + listOf(function.returnType)
            ),
            origin = IrStatementOrigin.LAMBDA,
            function = function
        )
    }

    fun IrExpression.isStatic(): Boolean {
        return when (this) {
            // A constant by definition is static
            is IrConst -> true
            // We want to consider all enum values as static
            is IrGetEnumValue -> true
            // Getting a companion object or top level object can be considered static if the
            // type of that object is Stable. (`Modifier` for instance is a common example)
            is IrGetObjectValue -> {
                if (symbol.owner.isCompanion) true
                else false
            }

            is IrConstructorCall -> isStatic()
            is IrCall -> isStatic()
            is IrGetValue -> {
                when (val owner = symbol.owner) {
                    is IrVariable -> {
                        // If we have an immutable variable whose initializer is also static,
                        // then we can determine that the variable reference is also static.
                        !owner.isVar && owner.initializer?.isStatic() == true
                    }

                    else -> false
                }
            }

            is IrFunctionExpression,
            is IrTypeOperatorCall -> false

            is IrGetField ->
                symbol.owner.correspondingPropertySymbol?.owner?.isConst == true

            is IrBlock ->  false
            else -> false
        }
    }

    private fun IrStatementOrigin?.isGetProperty() = this == IrStatementOrigin.GET_PROPERTY
    private fun IrStatementOrigin?.isSpecialCaseMathOp() =
        this in setOf(
            IrStatementOrigin.PLUS,
            IrStatementOrigin.MUL,
            IrStatementOrigin.MINUS,
            IrStatementOrigin.ANDAND,
            IrStatementOrigin.OROR,
            IrStatementOrigin.DIV,
            IrStatementOrigin.EQ,
            IrStatementOrigin.EQEQ,
            IrStatementOrigin.EQEQEQ,
            IrStatementOrigin.GT,
            IrStatementOrigin.GTEQ,
            IrStatementOrigin.LT,
            IrStatementOrigin.LTEQ
        )

    fun coerceInlineClasses(argument: IrExpression, from: IrType, to: IrType) =
        IrCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            to,
            unsafeCoerceIntrinsic!!
        ).apply {
            typeArguments[0] = from
            typeArguments[1] = to
            arguments[0] = argument
        }

    fun IrExpression.coerceToUnboxed() =
        coerceInlineClasses(this, this.type, this.type.unboxInlineClass())

    // Construct a reference to the JVM specific <unsafe-coerce> intrinsic.
    // This code should be kept in sync with the declaration in JvmSymbols.kt.
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol? by lazy {
        if (context.platform.isJvm()) {
            context.irFactory.buildFun {
                name = Name.special("<unsafe-coerce>")
                origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            }.apply {
                @Suppress("DEPRECATION")
                parent = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
                    context.moduleDescriptor,
                    FqName("kotlin.jvm.internal")
                )
                val src = addTypeParameter("T", context.irBuiltIns.anyNType)
                val dst = addTypeParameter("R", context.irBuiltIns.anyNType)
                addValueParameter("v", src.defaultType)
                returnType = dst.defaultType
            }.symbol
        } else {
            null
        }
    }

    /*
     * Delegated accessors are generated with IrReturn(IrCall(<delegated function>)) structure.
     * To verify the delegated function is composable, this function is unpacking it and
     * checks annotation on the symbol owner of the call.
     */
    fun IrFunction.isComposableDelegatedAccessor(): Boolean =
        origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR &&
                body?.let {
                    val returnStatement = it.statements.singleOrNull() as? IrReturn
                    val callStatement = returnStatement?.value as? IrCall
                    val target = callStatement?.symbol?.owner
                    target?.isTunable()
                } == true

    private val irEnumOrdinal =
        context.irBuiltIns.enumClass.owner.properties.single { it.name.asString() == "ordinal" }.getter!!

    private val protobufEnumClassId = ClassId.fromString("com/google/protobuf/Internal/EnumLite")

    private fun IrExpression.ordinalIfEnum(): IrExpression {
        val cls = type.classOrNull?.owner
        return when (cls?.kind) {
            ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY -> {
                val function = if (cls.isSubclassOf(context.referenceClass(protobufEnumClassId)?.owner ?: return this)) {
                    // For protobuf enums, we need to use the `getNumber` method instead of `ordinal`
                    cls.functions
                        .single {
                            it.name.asString() == "getNumber" &&
                                    it.parameters.size == 1 &&
                                    it.parameters[0].kind == IrParameterKind.DispatchReceiver
                        }
                } else {
                    irEnumOrdinal
                }
                if (type.isNullable()) {
                    val enumValue = irTemporary(this, "tmpEnum")
                    irBlock(
                        context.irBuiltIns.intType,
                        statements = listOf(
                            enumValue,
                            irIfThenElse(
                                type = context.irBuiltIns.intType,
                                condition = irEqual(irGet(enumValue), irNull()),
                                thenPart = irConst(-1),
                                elsePart = irCall(function.symbol, dispatchReceiver = irGet(enumValue))
                            )
                        )
                    )
                } else {
                    irCall(function.symbol, dispatchReceiver = this)
                }
            }
            else -> {
                this
            }
        }
    }

    fun IrStatement.wrap(
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset,
        type: IrType,
        before: List<IrStatement> = emptyList(),
        after: List<IrStatement> = emptyList(),
    ): IrContainerExpression {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            null,
            before + this + after
        )
    }

    private fun IrType.toPrimitiveType(): PrimitiveType? = when {
        isInt() -> PrimitiveType.INT
        isBoolean() -> PrimitiveType.BOOLEAN
        isFloat() -> PrimitiveType.FLOAT
        isLong() -> PrimitiveType.LONG
        isDouble() -> PrimitiveType.DOUBLE
        isByte() -> PrimitiveType.BYTE
        isChar() -> PrimitiveType.CHAR
        isShort() -> PrimitiveType.SHORT
        else -> null
    }

    fun irMethodCall(
        target: IrExpression,
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrCall {
        return irCall(function, startOffset, endOffset).apply {
            arguments[0] = target
        }
    }

    fun irCall(
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrCall {
        val type = function.returnType
        val symbol = function.symbol
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size
        )
    }



    /**
     *  Expressions for default values can use other parameters.
     *  In such cases we need to ensure that default values expressions use parameters of the new
     *  function (new/copied value parameters).
     *
     *  Example:
     *  fun Foo(a: String, b: String = a) {...}
     */
    private fun IrExpressionBody.transformDefaultValue(
        originalFunction: IrFunction,
        newFunction: IrFunction,
    ) {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val original = super.visitGetValue(expression)
                val parameter =
                    (expression.symbol.owner as? IrValueParameter) ?: return original

                val parameterIndex = parameter.indexInParameters
                if (parameter.parent != originalFunction) {
                    return super.visitGetValue(expression)
                }
                return IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    newFunction.parameters[parameterIndex].symbol,
                    expression.origin
                )
            }
        })
    }

}

fun IrAnnotationContainer.isTunable(): Boolean = this.hasAnnotation(TUNABLE_ANNOTATION_ID.asSingleFqName())
fun IrAnnotationContainer.isDisallowed(): Boolean = this.hasAnnotation(DISALLOW_TUNABLE_CALLS_ANNOTATION_ID.asSingleFqName())
fun IrType.isSyntheticTunableFunction() =
    classOrNull?.owner?.let {
        it.name.asString().startsWith("TunableFunction") &&
                it.packageFqName == FqName("com.huanli233.hibari.runtime.internal")
    } ?: false

fun IrType.isKTunableFunction() =
    classOrNull?.owner?.let {
        it.name.asString().startsWith("KTunableFunction") &&
                it.packageFqName == FqName("com.huanli233.hibari.runtime.internal")
    } ?: false
val AnnotationDescriptor.isTunableAnnotation: Boolean get() = fqName == TUNABLE_ANNOTATION_ID.asSingleFqName()
fun IrConstructorCall.isTunableAnnotation() =
    symbol.owner.constructedClass.hasEqualFqName(TUNABLE_ANNOTATION_ID.asSingleFqName())