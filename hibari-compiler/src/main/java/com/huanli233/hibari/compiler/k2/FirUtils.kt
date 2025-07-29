package com.huanli233.hibari.compiler.k2

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.huanli233.hibari.compiler.transformer.DISALLOW_TUNABLE_CALLS_ANNOTATION_ID
import com.huanli233.hibari.compiler.transformer.TUNABLE_ANNOTATION_ID
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.JvmStandardClassIds

fun FirAnnotationContainer.hasTunableAnnotation(session: FirSession): Boolean =
    hasAnnotation(TUNABLE_ANNOTATION_ID, session)

fun FirBasedSymbol<*>.hasTunableAnnotation(session: FirSession): Boolean =
    hasAnnotation(TUNABLE_ANNOTATION_ID, session)

fun FirAnnotationContainer.hasDisallowTunableCallsAnnotation(session: FirSession): Boolean =
    hasAnnotation(DISALLOW_TUNABLE_CALLS_ANNOTATION_ID, session)

fun FirAnnotationContainer.hasTunableTargetMarkerAnnotation(session: FirSession): Boolean =
    hasAnnotation(DISALLOW_TUNABLE_CALLS_ANNOTATION_ID, session)

fun FirCallableSymbol<*>.isTunable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasTunableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.let {
                it.hasTunableAnnotation(session) || it.isTunableDelegate(session)
            } ?: false
        else -> false
    }

fun FirValueParameterSymbol.isTunable(context: CheckerContext): Boolean =
    resolvedReturnType.customAnnotations.hasAnnotation(TUNABLE_ANNOTATION_ID, context.session) ||
            findSamFunction(context)?.isTunable(context.session) == true

private fun FirValueParameterSymbol.findSamFunction(context: CheckerContext): FirNamedFunctionSymbol? {
    val type = resolvedReturnType
    val session = context.session
    val classSymbol = type.toClassSymbol(session) ?: return null
    val samFunction = classSymbol
        .unsubstitutedScope(session, context.scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        .collectAllFunctions()
        .singleOrNull { it.modality == Modality.ABSTRACT }
    return samFunction
}

fun ConeKotlinType.isTunableFunction(session: FirSession): Boolean {
    val kind = functionTypeKind(session)
    return kind == TunableFunction || kind == KTunableFunction
}


@OptIn(SymbolInternals::class)
private fun FirPropertyAccessorSymbol.isTunableDelegate(session: FirSession): Boolean {
    if (!propertySymbol.hasDelegate) return false
    fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    return ((fir
        .body
        ?.statements
        ?.singleOrNull() as? FirReturnExpression)
        ?.result as? FirFunctionCall)
        ?.calleeReference
        ?.toResolvedCallableSymbol()
        ?.isTunable(session)
        ?: false
}

// TODO: Replace this with the FIR MainFunctionDetector once it lands upstream!
fun FirFunctionSymbol<*>.isMain(session: FirSession): Boolean {
    if (this !is FirNamedFunctionSymbol) return false
    if (typeParameterSymbols.isNotEmpty()) return false
    if (!resolvedReturnType.isUnit) return false
    if (jvmNameAsString(session) != "main") return false

    val parameterTypes = explicitParameterTypes
    when (parameterTypes.size) {
        0 -> {
            /*
            assert(DescriptorUtils.isTopLevelDeclaration(descriptor)) { "main without parameters works only for top-level" }
            val containingFile = DescriptorToSourceUtils.getContainingFile(descriptor)
            // We do not support parameterless entry points having JvmName("name") but different real names
            // See more at https://github.com/Kotlin/KEEP/blob/master/proposals/enhancing-main-convention.md#parameterless-main
            if (descriptor.name.asString() != "main") return false
            if (containingFile?.declarations?.any { declaration -> isMainWithParameter(declaration, checkJvmStaticAnnotation) } == true) {
                return false
            }*/
        }
        1 -> {
            val type = parameterTypes.single()
            if (!type.isArrayType || type.typeArguments.size != 1) return false
            val elementType = type.typeArguments[0].takeIf { it.kind != ProjectionKind.IN }?.type
                ?: return false
            if (!elementType.isString) return false
        }
        else -> return false
    }
    /*
    if (DescriptorUtils.isTopLevelDeclaration(descriptor)) return true

    val containingDeclaration = descriptor.containingDeclaration
    return containingDeclaration is ClassDescriptor
            && containingDeclaration.kind.isSingleton
            && (descriptor.hasJvmStaticAnnotation() || !checkJvmStaticAnnotation)
     */
    return true
}

private fun FirNamedFunctionSymbol.jvmNameAsString(session: FirSession): String =
    getAnnotationStringParameter(JvmStandardClassIds.Annotations.JvmName, session)
        ?: name.asString()

private val FirFunctionSymbol<*>.explicitParameterTypes: List<ConeKotlinType>
    get() = listOfNotNull(receiverParameter?.symbol?.resolvedType) +
            valueParameterSymbols.map { it.resolvedReturnType }