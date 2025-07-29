package com.huanli233.hibari.compiler.k2

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE_WITH_ANNOTATIONS

object HibariErrorMessages : BaseDiagnosticRendererFactory() {

    override val MAP = KtDiagnosticFactoryToRendererMap("Hibari").apply {
        put(
            HibariErrors.TUNABLE_INVOCATION,
            "@Tunable invocations can only happen from the context of a @Tunable function"
        )

        put(
            HibariErrors.TUNABLE_EXPECTED,
            "Functions which invoke @Tunable functions must be marked with the @Tunable " +
                    "annotation"
        )

        put(
            HibariErrors.TUNABLE_FUNCTION_REFERENCE,
            "Function References of @Tunable functions are not currently supported"
        )

        put(
            HibariErrors.CAPTURED_TUNABLE_INVOCATION,
            "Tunable calls are not allowed inside the {0} parameter of {1}",
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.SYMBOL
        )

        put(
            HibariErrors.MISSING_DISALLOW_TUNABLE_CALLS_ANNOTATION,
            "Parameter {0} cannot be inlined inside of lambda argument {1} of {2} " +
                    "without also being annotated with @DisallowTunableCalls",
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.SYMBOL
        )

        put(
            HibariErrors.NONREADONLY_CALL_IN_READONLY_TUNABLE,
            "Tunables marked with @ReadOnlyTunable can only call other @ReadOnlyTunable " +
                    "composables"
        )

        put(
            HibariErrors.TUNABLE_PROPERTY_BACKING_FIELD,
            "Tunable properties are not able to have backing fields"
        )

        put(
            HibariErrors.CONFLICTING_OVERLOADS,
            "@Tunable annotation mismatch with overridden function: {0}",
            CommonRenderers.commaSeparated(
                Renderers.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS
            )
        )

        put(
            HibariErrors.TUNABLE_VAR,
            "Tunable properties are not able to have backing fields"
        )
        put(
            HibariErrors.TUNABLE_SUSPEND_FUN,
            "Tunable function cannot be annotated as suspend"
        )
        put(
            HibariErrors.ABSTRACT_TUNABLE_DEFAULT_PARAMETER_VALUE,
            "Open Tunable functions with default values are only supported with language version 2.0 or higher."
        )
        put(
            HibariErrors.TUNABLE_FUN_MAIN,
            "Tunable main functions are not currently supported"
        )
        put(
            HibariErrors.ILLEGAL_TRY_CATCH_AROUND_TUNABLE,
            "Try catch is not supported around composable function invocations."
        )
        put(
            HibariErrors.TYPE_MISMATCH,
            "Type inference failed. Expected type mismatch: inferred type is {1} but {0}" +
                    " was expected",
            RENDER_TYPE_WITH_ANNOTATIONS,
            RENDER_TYPE_WITH_ANNOTATIONS
        )
        put(
            HibariErrors.HIBARI_APPLIER_CALL_MISMATCH,
            "Calling a {1} composable function where a {0} composable was expected",
            Renderers.TO_STRING,
            Renderers.TO_STRING
        )
        put(
            HibariErrors.HIBARI_APPLIER_PARAMETER_MISMATCH,
            "A {1} composable parameter was provided where a {0} composable was expected",
            Renderers.TO_STRING,
            Renderers.TO_STRING
        )
        put(
            HibariErrors.HIBARI_APPLIER_DECLARATION_MISMATCH,
            "The composition target of an override must match the ancestor target"
        )
        put(
            HibariErrors.HIBARI_INVALID_DELEGATE,
            "Tunable setValue operator is not currently supported."
        )
        put(
            HibariErrors.MISMATCHED_TUNABLE_IN_EXPECT_ACTUAL,
            "Mismatched @Tunable annotation between expect and actual declaration"
        )
        put(
            HibariErrors.REDUNDANT_TUNABLE_ANNOTATION,
            "Invalid `@Tunable` annotation on inline lambda." +
                    " This will become an error in Kotlin 2.0."
        )
        put(
            HibariErrors.NAMED_ARGUMENTS_NOT_ALLOWED,
            "Named arguments in composable function types are deprecated." +
                    " This will become an error in Kotlin 2.0"
        )
        put(
            HibariErrors.TUNABLE_INAPPLICABLE_TYPE,
            "@Tunable annotation is not applicable to {0}",
            RENDER_TYPE
        )
    }

}