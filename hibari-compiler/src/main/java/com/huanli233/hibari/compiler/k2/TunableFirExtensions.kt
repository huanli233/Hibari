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

import com.huanli233.hibari.compiler.transformer.TUNABLE_ANNOTATION_ID
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.name.FqName

class HibariFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::HibariFunctionTypeKindExtension
        +::HibariFirCheckersExtension
        +::TunableTargetSessionStorage
    }
}

class HibariFunctionTypeKindExtension(
    session: FirSession,
) : FirFunctionTypeKindExtension(session) {
    override fun FunctionTypeKindRegistrar.registerKinds() {
        registerKind(TunableFunction, KTunableFunction)
    }
}

private val useLegacyCustomFunctionTypeSerializationUntil: String
    get() {
        require(!LanguageVersion.entries.last().isStable) {
            "Last value in `LanguageVersion` enum is not expected to be a stable version."
        }
        return LanguageVersion.entries.last().versionString
    }

object TunableFunction : FunctionTypeKind(
    FqName("com.huanli233.hibari.runtime.internal"),
    "TunableFunction",
    TUNABLE_ANNOTATION_ID,
    isReflectType = false,
    isInlineable = true,
) {
    override val prefixForTypeRender: String = "@Tunable"

    override val serializeAsFunctionWithAnnotationUntil: String = useLegacyCustomFunctionTypeSerializationUntil

    override fun reflectKind(): FunctionTypeKind = KTunableFunction
}

object KTunableFunction : FunctionTypeKind(
    FqName("com.huanli233.hibari.runtime.internal"),
    "KTunableFunction",
    TUNABLE_ANNOTATION_ID,
    isReflectType = true,
    isInlineable = false,
) {
    override val prefixForTypeRender: String = "@Tunable"

    override val serializeAsFunctionWithAnnotationUntil: String = useLegacyCustomFunctionTypeSerializationUntil

    override fun nonReflectKind(): FunctionTypeKind = TunableFunction
}

class HibariFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val functionCheckers: Set<FirFunctionChecker> =
            setOf(TunableFunctionChecker)

        override val propertyCheckers: Set<FirPropertyChecker> =
            setOf(TunablePropertyChecker)
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> =
            setOf(TunableAnnotationChecker)
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> =
            setOf(TunableFunctionCallChecker, TunableTargetChecker)

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> =
            setOf(TunablePropertyAccessExpressionChecker)

        override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> =
            setOf(TunablePropertyReferenceChecker)
    }

}