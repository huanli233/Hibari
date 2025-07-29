package com.huanli233.hibari.compiler.k2

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning2
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression

object HibariErrors {

    val TUNABLE_INVOCATION by error0<PsiElement>()

    val TUNABLE_EXPECTED by error0<PsiElement>()

    val TUNABLE_FUNCTION_REFERENCE by error0<KtCallableReferenceExpression>()

    val TUNABLE_PROPERTY_BACKING_FIELD by error0<PsiElement>()

    val TUNABLE_VAR by error0<PsiElement>()

    val TUNABLE_SUSPEND_FUN by error0<PsiElement>()

    val ABSTRACT_TUNABLE_DEFAULT_PARAMETER_VALUE by error0<PsiElement>()

    val TUNABLE_FUN_MAIN by error0<PsiElement>()

    val CAPTURED_TUNABLE_INVOCATION by error2<PsiElement, FirBasedSymbol<*>, FirBasedSymbol<*>>()

    val MISSING_DISALLOW_TUNABLE_CALLS_ANNOTATION by error3<
            PsiElement,
            FirBasedSymbol<*>, // unmarked
            FirBasedSymbol<*>, // marked
            FirBasedSymbol<*>
            >()

    val NONREADONLY_CALL_IN_READONLY_TUNABLE by error0<PsiElement>()

    val CONFLICTING_OVERLOADS by error1<PsiElement, Collection<DeclarationDescriptor>>(
        SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
    )

    val ILLEGAL_TRY_CATCH_AROUND_TUNABLE by error0<PsiElement>()

    val TYPE_MISMATCH by error2<KtExpression, ConeKotlinType, ConeKotlinType>()

    val HIBARI_APPLIER_CALL_MISMATCH by warning2<PsiElement, String, String>()

    val HIBARI_APPLIER_PARAMETER_MISMATCH by warning2<PsiElement, String, String>()

    val HIBARI_APPLIER_DECLARATION_MISMATCH by warning0<PsiElement>()

    val HIBARI_INVALID_DELEGATE by error0<PsiElement>()

    val NAMED_ARGUMENTS_NOT_ALLOWED by warning0<PsiElement>()

    val MISMATCHED_TUNABLE_IN_EXPECT_ACTUAL by error0<PsiElement>()

    val REDUNDANT_TUNABLE_ANNOTATION by warning0<PsiElement>()

    val TUNABLE_INAPPLICABLE_TYPE by error1<PsiElement, ConeKotlinType>()

    init {
        RootDiagnosticRendererFactory.registerFactory(HibariErrorMessages)
    }

}
