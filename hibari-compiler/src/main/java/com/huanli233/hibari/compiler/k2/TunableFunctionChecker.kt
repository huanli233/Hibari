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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.util.OperatorNameConventions

object TunableFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val isComposable = declaration.hasTunableAnnotation(context.session)

        // Check that `actual` composable declarations have composable expects
        declaration.symbol.getSingleMatchedExpectForActualOrNull()?.let { expectDeclaration ->
            if (expectDeclaration.hasTunableAnnotation(context.session) != isComposable) {
                reporter.reportOn(
                    declaration.source,
                    HibariErrors.MISMATCHED_TUNABLE_IN_EXPECT_ACTUAL,
                    context
                )
            }
        }

        if (!isComposable) return

        // Composable suspend functions are unsupported
        if (declaration.isSuspend) {
            reporter.reportOn(declaration.source, HibariErrors.TUNABLE_SUSPEND_FUN, context)
        }

        // Composable main functions are not allowed.
        if (declaration.symbol.isMain(context.session)) {
            reporter.reportOn(declaration.source, HibariErrors.TUNABLE_FUN_MAIN, context)
        }

        // Disallow composable setValue operators
        if (declaration.isOperator &&
            declaration.nameOrSpecialName == OperatorNameConventions.SET_VALUE
        ) {
            reporter.reportOn(declaration.source, HibariErrors.HIBARI_INVALID_DELEGATE, context)
        }
    }
}

