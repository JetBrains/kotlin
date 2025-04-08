/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestHiddenContextParameters(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - top level function is hidden`() {
        doTest(
            source = """
            class Context

            context(contextName: Context)
            fun topLevelFoo() = Unit
        """, expected = """
                #import <Foundation/NSArray.h>
                #import <Foundation/NSDictionary.h>
                #import <Foundation/NSError.h>
                #import <Foundation/NSObject.h>
                #import <Foundation/NSSet.h>
                #import <Foundation/NSString.h>
                #import <Foundation/NSValue.h>

                NS_ASSUME_NONNULL_BEGIN
                #pragma clang diagnostic push
                #pragma clang diagnostic ignored "-Wunknown-warning-option"
                #pragma clang diagnostic ignored "-Wincompatible-property-type"
                #pragma clang diagnostic ignored "-Wnullability"

                #pragma push_macro("_Nullable_result")
                #if !__has_feature(nullability_nullable_result)
                #undef _Nullable_result
                #define _Nullable_result _Nullable
                #endif

                __attribute__((objc_subclassing_restricted))
                @interface Context : Base
                - (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
                + (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
                @end
                
                #pragma pop_macro("_Nullable_result")
                #pragma clang diagnostic pop
                NS_ASSUME_NONNULL_END
            """.trimIndent()
        )
    }

    @Test
    fun `test - top member function is hidden`() {
        doTest(
            source = """
            class Context

            class Bar {
                context(contextName: Context)
                fun memberFoo() = Unit
            }
        """, expected = """
                #import <Foundation/NSArray.h>
                #import <Foundation/NSDictionary.h>
                #import <Foundation/NSError.h>
                #import <Foundation/NSObject.h>
                #import <Foundation/NSSet.h>
                #import <Foundation/NSString.h>
                #import <Foundation/NSValue.h>

                NS_ASSUME_NONNULL_BEGIN
                #pragma clang diagnostic push
                #pragma clang diagnostic ignored "-Wunknown-warning-option"
                #pragma clang diagnostic ignored "-Wincompatible-property-type"
                #pragma clang diagnostic ignored "-Wnullability"

                #pragma push_macro("_Nullable_result")
                #if !__has_feature(nullability_nullable_result)
                #undef _Nullable_result
                #define _Nullable_result _Nullable
                #endif

                __attribute__((objc_subclassing_restricted))
                @interface Bar : Base
                - (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
                + (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
                @end
                
                __attribute__((objc_subclassing_restricted))
                @interface Context : Base
                - (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
                + (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
                @end
                
                #pragma pop_macro("_Nullable_result")
                #pragma clang diagnostic pop
                NS_ASSUME_NONNULL_END
            """.trimIndent()
        )
    }

    private fun doTest(
        @Language("kotlin") source: String,
        @Language("c") expected: String,
    ) {
        val file = inlineSourceCodeAnalysis.createKtFile(source)
        analyze(file) {
            with(
                ObjCExportContext(
                    analysisSession = this, exportSession = KtObjCExportSessionImpl(
                        KtObjCExportConfiguration(),
                        moduleNaming = KtObjCExportModuleNaming.default,
                        moduleClassifier = KtObjCExportModuleClassifier.default,
                        cache = hashMapOf(),
                        overrides = hashMapOf()
                    )
                )
            ) {
                val actual = translateToObjCHeader(listOf(KtObjCExportFile(file)), false).toString()
                assertEquals(
                    expected, actual
                )
            }
        }
    }
}