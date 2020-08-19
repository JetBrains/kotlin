package translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Bug1341 : AbstractCoreTest() {
    @Test
    fun `reproduce bug #1341`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                }
            }
        }

        testInline(
            """
            /src/com/sample/OtherClass.kt
            package com.sample
            /**
             * @suppress
             */
            class OtherClass internal constructor() {
                @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
                @IntDef(ELEM_1, ELEM_2, ELEM_3)
                internal annotation class CustomAnnotation

                companion object {
                    const val ELEM_1 = 1
                    const val ELEM_2 = -1
                    const val ELEM_3 = 0
                }
            }
            
            /src/com/sample/ClassUsingAnnotation.java
            package com.sample
            import static sample.OtherClass.ELEM_1;

            /**
             * @suppress
             */
            public class ClassUsingAnnotation {

                @OtherClass.CustomAnnotation
                public int doSomething() {
                    return ELEM_1;
                }
            }
            """.trimIndent(),
            configuration
        ) {
            this.documentablesMergingStage = { module ->
                assertEquals(DRI("com.sample"), module.packages.single().dri)
            }
        }
    }
}
