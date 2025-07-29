package com.huanli233.hibari.compiler.logging

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

fun MessageCollector.info(message: String) {
    report(CompilerMessageSeverity.INFO, message)
}

fun MessageCollector.warn(message: String) {
    report(CompilerMessageSeverity.WARNING, message)
}

fun MessageCollector.error(message: String) {
    report(CompilerMessageSeverity.ERROR, message)
}

fun MessageCollector.logging(message: String) {
    report(CompilerMessageSeverity.LOGGING, message)
}

fun MessageCollector.output(message: String) {
    report(CompilerMessageSeverity.OUTPUT, message)
}
