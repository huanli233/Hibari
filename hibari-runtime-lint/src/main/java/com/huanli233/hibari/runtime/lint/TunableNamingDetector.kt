package com.huanli233.hibari.runtime.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import java.util.EnumSet
import java.util.Locale

class TunableNamingDetector: Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "TunableNamingDetector",
            briefDescription = "Tunable naming detector",
            explanation = "Tunable naming detector",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.WARNING,
            implementation = Implementation(
                TunableNamingDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>?  = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!node.isTunable) return

                if (context.evaluator.isOperator(node)) return

                if (node.findSuperMethods().isNotEmpty()) return

                val name = node.name

                val capitalizedFunctionName = name.first().isUpperCase()

                if (node.returnsUnit) {
                    if (!capitalizedFunctionName) {
                        val capitalizedName =
                            name.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            }
                        context.report(
                            ISSUE,
                            node,
                            context.getNameLocation(node),
                            "Tunable functions that return Unit should start with an " +
                                    "uppercase letter",
                            LintFix.create()
                                .replace()
                                .name("Change to $capitalizedName")
                                .text(name)
                                .with(capitalizedName)
                                .autoFix()
                                .build(),
                        )
                    }
                } else {
                    if (capitalizedFunctionName) {
                        val lowercaseName =
                            name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                        context.report(
                            ISSUE,
                            node,
                            context.getNameLocation(node),
                            "Tunable functions with a return type should start with a " +
                                    "lowercase letter",
                            LintFix.create()
                                .replace()
                                .name("Change to $lowercaseName")
                                .text(name)
                                .with(lowercaseName)
                                .autoFix()
                                .build(),
                        )
                    }
                }
            }
        }
}

val PsiMethod.returnsUnit
    get() = returnType.isVoidOrUnit

val PsiType?.isVoidOrUnit
    get() = this == PsiTypes.voidType() || this?.canonicalText == "kotlin.Unit"