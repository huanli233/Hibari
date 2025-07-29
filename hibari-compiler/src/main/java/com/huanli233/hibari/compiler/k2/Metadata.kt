package com.huanli233.hibari.compiler.k2

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry

private object CompilerPluginMetadata : FirDeclarationDataKey()
var FirDeclaration.hibariCompilerPluginMetadata: Map<String, ByteArray>? by FirDeclarationDataRegistry.data(CompilerPluginMetadata)