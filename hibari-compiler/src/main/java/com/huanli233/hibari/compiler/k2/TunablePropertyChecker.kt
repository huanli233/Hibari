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

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol

object TunablePropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.getter?.hasTunableAnnotation(context.session) != true &&
            declaration.setter?.hasTunableAnnotation(context.session) != true
        ) {
            return
        }

        if (declaration.isVar) {
            reporter.reportOn(declaration.source, HibariErrors.TUNABLE_VAR, context)
        }

        if (declaration.hasBackingField) {
            reporter.reportOn(
                declaration.source,
                HibariErrors.TUNABLE_PROPERTY_BACKING_FIELD,
                context
            )
        }
    }
}

object TunablePropertyReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    override fun check(expression: FirCallableReferenceAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val property = expression.calleeReference.toResolvedPropertySymbol()
        if (
            property != null
            && !property.hasDelegate
            && property.isTunable(context.session)
        ) {
            reporter.reportOn(
                expression.source,
                HibariErrors.TUNABLE_FUNCTION_REFERENCE,
                context
            )
        }
    }

}