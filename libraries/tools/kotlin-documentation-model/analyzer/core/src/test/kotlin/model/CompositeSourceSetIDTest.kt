package model

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.model.CompositeSourceSetID
import org.jetbrains.dokka.model.plus
import kotlin.test.*

class CompositeSourceSetIDTest {

    @Test
    fun `constructor fails with empty collection`() {
        assertFailsWith<IllegalArgumentException>("Expected no construction of empty `CompositeSourceSetID`") {
            CompositeSourceSetID(emptyList())
        }
    }

    @Test
    fun `merged for single source set`() {
        val sourceSetID = DokkaSourceSetID("module", "sourceSet")
        val composite = CompositeSourceSetID(sourceSetID)

        assertEquals(
            composite.merged, sourceSetID,
            "Expected merged source set id to be equal to single child"
        )
    }

    @Test
    fun `merged with multiple source sets`() {
        val composite = CompositeSourceSetID(
            listOf(DokkaSourceSetID("m1", "s1"), DokkaSourceSetID("m2", "s2"), DokkaSourceSetID("m3", "s3"))
        )

        assertEquals(
            DokkaSourceSetID("m1+m2+m3", "s1+s2+s3"), composite.merged,
            "Expected merged source set id to concatenate source sets"
        )
    }

    @Test
    fun `contains with child sourceSetID`() {
        val composite = CompositeSourceSetID(listOf(DokkaSourceSetID("m1", "s1"), DokkaSourceSetID("m2", "s2")))

        assertFalse(
            DokkaSourceSetID("m3", "s3") in composite,
            "Expected source set id not being contained in composite"
        )

        assertTrue(
            DokkaSourceSetID("m1", "s1") in composite,
            "Expected child source set id being contained in composite"
        )

        assertTrue(
            DokkaSourceSetID("m1+m2", "s1+s2") in composite,
            "Expected merged source set id being contained in composite"
        )
    }

    @Test
    fun `plus operator`() {
        val composite = DokkaSourceSetID("m1", "s1") + DokkaSourceSetID("m2", "s2") + DokkaSourceSetID("m3", "s3")
        assertEquals(
            DokkaSourceSetID("m1+m2+m3", "s1+s2+s3"), composite.merged,
            "Expected all three source sets being merged in order"
        )
    }
}
