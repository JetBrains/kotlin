package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.KmAnnotationArgument
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmModuleFragment
import kotlinx.metadata.klib.compileTimeValue
import kotlinx.metadata.klib.uniqId
import org.jetbrains.kotlin.native.interop.indexer.FunctionDecl
import org.jetbrains.kotlin.native.interop.indexer.IntegerConstantDef
import org.jetbrains.kotlin.native.interop.indexer.IntegerType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StubIrToMetadataTests {

    companion object {
        val intStubType = ClassifierStubType(Classifier.topLevel("kotlin", "Int"))
        val intType = IntegerType(4, true, "int")
    }

    private fun createTrivialFunction(name: String): FunctionStub {
        val cDeclaration = FunctionDecl(name, emptyList(), intType, "", false, false)
        val origin = StubOrigin.Function(cDeclaration)
        return FunctionStub(
                name = cDeclaration.name,
                returnType = intStubType,
                parameters = listOf(),
                origin = origin,
                annotations = emptyList(),
                external = true,
                receiver = null,
                modality = MemberStubModality.FINAL
        )
    }

    private fun createTrivialIntegerConstantProperty(name: String, value: Long): PropertyStub {
        val origin = StubOrigin.Constant(IntegerConstantDef(name, intType, value))
        return PropertyStub(
                name = name,
                type = intStubType,
                kind = PropertyStub.Kind.Constant(IntegralConstantStub(value, intType.size, true)),
                origin = origin
        )
    }

    private fun createMetadata(
            fqName: String,
            functions: List<FunctionStub> = emptyList(),
            properties: List<PropertyStub> = emptyList()
    ) = SimpleStubContainer(functions = functions, properties = properties)
            .let { ModuleMetadataEmitter(fqName, it).emit() }
            .also(this::checkUniqIdPresence)

    private fun checkUniqIdPresence(metadata: KmModuleFragment) {
        metadata.classes.forEach { assertNotNull(it.uniqId) }
        metadata.pkg?.let { pkg ->
            pkg.functions.forEach { assertNotNull(it.uniqId) }
            pkg.properties.forEach { assertNotNull(it.uniqId) }
            pkg.typeAliases.forEach { assertNotNull(it.uniqId) }
        }
    }

    @Test
    fun `single simple function`() {
        val packageName = "single_function"
        val function = createTrivialFunction("hello")
        val metadata = createMetadata(packageName, functions = listOf(function))
        with (metadata) {
            assertEquals(packageName, packageName)
            assertTrue(classes.isEmpty())
            assertNotNull(pkg)
            assertTrue(pkg!!.functions.size == 1)

            val kmFunction = pkg!!.functions[0]
            assertEquals(kmFunction.name, function.name)
            assertEquals(0, kmFunction.valueParameters.size)
            val returnTypeClassifier = kmFunction.returnType.classifier
            assertTrue(returnTypeClassifier is KmClassifier.Class)
            assertEquals("kotlin/Int", returnTypeClassifier.name)
        }
    }

    @Test
    fun `single constant`() {
        val property = createTrivialIntegerConstantProperty("meaning", 42)
        val metadata = createMetadata("single_property", properties = listOf(property))
        with (metadata) {
            assertNotNull(pkg)
            assertTrue(pkg!!.properties.size == 1)

            val kmProperty = pkg!!.properties[0]
            assertEquals(kmProperty.name, property.name)

            val compileTimeValue = kmProperty.compileTimeValue
            assertNotNull(compileTimeValue)
            assertTrue(compileTimeValue is KmAnnotationArgument.IntValue)
            assertEquals(42, compileTimeValue.value)
        }
    }
}