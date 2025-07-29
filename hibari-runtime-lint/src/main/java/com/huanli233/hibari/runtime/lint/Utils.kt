package com.huanli233.hibari.runtime.lint

import com.intellij.psi.PsiMethod

val PsiMethod.isTunable
    get() = hasAnnotation(Names.Runtime.TUNABLE)