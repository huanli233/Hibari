/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.huanli233.hibari.compiler.k2

import com.huanli233.hibari.compiler.transformer.TUNABLE_ANNOTATION_ID
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isNonPrimitiveArray

object TunableAnnotationChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val composableAnnotation = typeRef.getAnnotationByClassId(TUNABLE_ANNOTATION_ID, context.session)
        if (composableAnnotation == null) {
            return
        }

        // ignore if the type is coming from vararg
        if (
            typeRef.delegatedTypeRef?.source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter &&
            typeRef.coneType.isNonPrimitiveArray
        ) {
            return
        }

        if (
            typeRef !is FirErrorTypeRef &&
            !typeRef.coneType.isTunableFunction(context.session)
        ) {
            reporter.reportOn(
                composableAnnotation.source,
                HibariErrors.TUNABLE_INAPPLICABLE_TYPE,
                typeRef.coneType,
                context
            )
        }
    }
}