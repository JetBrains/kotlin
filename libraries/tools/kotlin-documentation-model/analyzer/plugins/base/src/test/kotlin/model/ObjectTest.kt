package model

import org.jetbrains.dokka.model.AdditionalModifiers
import org.jetbrains.dokka.model.DObject
import org.jetbrains.dokka.model.ExtraModifiers
import utils.AbstractModelTest
import kotlin.test.Test

class ObjectTest : AbstractModelTest("/src/main/kotlin/objects/Test.kt", "objects") {

    @Test
    fun emptyObject() {
        inlineModelTest(
            """
            |object Obj {}
            """.trimIndent()
        ) {
            with((this / "objects" / "Obj").cast<DObject>()) {
                name equals "Obj"
                children counts 3
            }
        }
    }

    @Test
    fun `data object class`() {
        inlineModelTest(
            """
            |data object KotlinDataObject {}
            """.trimIndent()
        ) {
            with((this / "objects" / "KotlinDataObject").cast<DObject>()) {
                name equals "KotlinDataObject"
                extra[AdditionalModifiers]?.content?.values?.single()
                    ?.single() equals ExtraModifiers.KotlinOnlyModifiers.Data
            }
        }
    }
}
