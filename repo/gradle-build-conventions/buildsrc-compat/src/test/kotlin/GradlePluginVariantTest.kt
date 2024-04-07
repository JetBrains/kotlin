import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GradlePluginVariantTest {

    @Test
    fun testLatestGradleVariantUsesCurrentDocs() {
        val lastVariant = GradlePluginVariant.values().last()

        assertEquals(
            "https://docs.gradle.org/current/javadoc/",
            lastVariant.gradleApiJavadocUrl
        )
    }

    @Test
    fun testNonLatestGradleVariantDoesNotUseCurrentDocs() {
        GradlePluginVariant.values().dropLast(1).forEach {
            assertNotEquals(
                "https://docs.gradle.org/current/javadoc/",
                it.gradleApiJavadocUrl
            )
        }
    }
}