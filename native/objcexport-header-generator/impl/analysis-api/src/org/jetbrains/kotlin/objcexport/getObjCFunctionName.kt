package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getObjCFunctionName(): ObjCExportFunctionName {
    val resolveObjCNameAnnotation = resolveObjCNameAnnotation()


    val name: String = when (this) {
            is KtFunctionSymbol -> this.name.asString()
            is KtConstructorSymbol -> "init"
            is KtPropertyGetterSymbol -> "get" //TODO: implement properly getter since it doesn't have [name]
            else -> "Undefined name for $this type"
        }

        return ObjCExportFunctionName(
             resolveObjCNameAnnotation?.swiftName ?:  name,  resolveObjCNameAnnotation?.objCName ?: name
        )
}