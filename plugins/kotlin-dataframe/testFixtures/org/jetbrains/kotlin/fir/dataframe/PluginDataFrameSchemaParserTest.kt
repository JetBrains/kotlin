package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PluginDataFrameSchemaParserTest {

    private val parser = PluginDataFrameSchemaParser()

    @Test
    fun `should parse simple schema with basic types`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "simple.json",
          "schema": {
            "id": "kotlin.Int",
            "name": "kotlin.String"
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)
        assertEquals("org.jetbrains.kotlinx.dataframe.io.JSON", result.metadata.format)
        assertEquals("simple.json", result.metadata.data)
        assertEquals(2, result.schema.columns().size)

        val idColumn = result.schema.columns()[0]
        assertIs<SimpleDataColumn>(idColumn)
        assertEquals("id", idColumn.name)
        assertEquals(StandardClassIds.Int.constructClassLikeType().wrap(), idColumn.type)
    }

    @Test
    fun `should parse schema with nullable types`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.CSV",
          "data": "data.csv",
          "schema": {
            "nullableInt": "kotlin.Int?"
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)
        assertEquals(1, result.schema.columns().size)

        val nullableInt = result.schema.columns()[0]
        assertIs<SimpleDataColumn>(nullableInt)
        assertEquals(StandardClassIds.Int.constructClassLikeType(isMarkedNullable = true).wrap(), nullableInt.type)
    }

    @Test
    fun `should parse schema with column groups`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "grouped.json",
          "schema": {
            "userInfo: ColumnGroup": {
              "firstName": "kotlin.String",
              "lastName": "kotlin.String",
              "age": "kotlin.Int"
            }
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)
        assertEquals(1, result.schema.columns().size)

        val groupColumn = result.schema.columns()[0]
        assertIs<SimpleColumnGroup>(groupColumn)
        assertEquals("userInfo", groupColumn.name)
        assertEquals(3, groupColumn.columns().size)
    }

    @Test
    fun `should parse schema with nested column groups`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "nested.json",
          "schema": {
            "id": "kotlin.Int",
            "data: ColumnGroup": {
              "value": "kotlin.String",
              "metadata: ColumnGroup": {
                "created": "kotlin.String",
                "modified": "kotlin.String"
              }
            }
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)

        val dataGroup = result.schema.columns()[1]
        assertIs<SimpleColumnGroup>(dataGroup)
        assertEquals("data", dataGroup.name)

        val nestedGroup = dataGroup.columns()[1]
        assertIs<SimpleColumnGroup>(nestedGroup)
        assertEquals("metadata", nestedGroup.name)
        assertEquals(2, nestedGroup.columns().size)
    }

    @Test
    fun `should parse schema with frame columns`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "frames.json",
          "schema": {
            "id": "kotlin.Int",
            "items: FrameColumn": {
              "name": "kotlin.String",
              "quantity": "kotlin.Int",
              "price": "kotlin.Double"
            }
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)

        val frameColumn = result.schema.columns()[1]
        assertIs<SimpleFrameColumn>(frameColumn)
        assertEquals("items", frameColumn.name)
        assertEquals(3, frameColumn.columns().size)
    }

    @Test
    fun `should parse complex schema with mixed column types`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "complex.json",
          "schema": {
            "id": "kotlin.Int",
            "name": "kotlin.String",
            "address: ColumnGroup": {
              "street": "kotlin.String",
              "city": "kotlin.String",
              "coordinates: ColumnGroup": {
                "lat": "kotlin.Double",
                "lon": "kotlin.Double"
              }
            },
            "orders: FrameColumn": {
              "orderId": "kotlin.Long",
              "amount": "kotlin.Double",
              "items: FrameColumn": {
                "productId": "kotlin.Int",
                "quantity": "kotlin.Int"
              }
            }
          }
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)
        assertEquals(4, result.schema.columns().size)

        val addressGroup = result.schema.columns()[2]
        assertIs<SimpleColumnGroup>(addressGroup)
        val coordinatesGroup = addressGroup.columns()[2]
        assertIs<SimpleColumnGroup>(coordinatesGroup)
        assertEquals("coordinates", coordinatesGroup.name)

        val ordersFrame = result.schema.columns()[3]
        assertIs<SimpleFrameColumn>(ordersFrame)
        val nestedItemsFrame = ordersFrame.columns()[2]
        assertIs<SimpleFrameColumn>(nestedItemsFrame)
        assertEquals("items", nestedItemsFrame.name)
    }

    @Test
    fun `should parse empty schema`() {
        val json = """
        {
          "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
          "data": "empty.json",
          "schema": {}
        }
        """.trimIndent()

        val result = parser.parseSchemaWithMeta(json)

        assertIs<ImportedDataSchema>(result)
        assertTrue(result.schema.columns().isEmpty())
    }

    @Nested
    inner class ParsingProblemsDontCauseExceptions {
        @Test
        fun `should fail when schema field is missing`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json"
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail when format field is missing`() {
            val json = """
            {
              "data": "test.json",
              "schema": {
                "id": "kotlin.Int"
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail when data field is missing`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "schema": {
                "id": "kotlin.Int"
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail with invalid JSON`() {
            val json = """
            {
              "format": "test",
              "data": "test.json",
              "schema": {
                invalid json here
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail with blank column name`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json",
              "schema": {
                "": "kotlin.Int"
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail with blank type string`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json",
              "schema": {
                "id": ""
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail when column group has no nested columns`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json",
              "schema": {
                "empty: ColumnGroup": {}
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }

        @Test
        fun `should fail when nested object lacks proper suffix`() {
            val json = """
            {
              "format": "org.jetbrains.kotlinx.dataframe.io.JSON",
              "data": "test.json",
              "schema": {
                "invalidNested": {
                  "field": "kotlin.String"
                }
              }
            }
            """.trimIndent()

            val result = parser.parseSchemaWithMeta(json)

            assertIs<ParseResult.Failure>(result)
        }
    }
}