package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.string
import org.jetbrains.kotlinx.dataframe.plugin.type
import kotlin.reflect.KClass

//@Serializable
//public sealed interface TypeApproximation {
//    public companion object
//}

public typealias TypeApproximation = org.jetbrains.kotlinx.dataframe.Marker

//@Serializable
//public data class TypeApproximationImpl(public val fqName: String, public val nullable: Boolean) : TypeApproximation

//public fun TypeApproximation(fqName: String, nullable: Boolean): TypeApproximation = TypeApproximationImpl(fqName, nullable)

//@Serializable

//@Serializable
//public object FrameColumnTypeApproximation : TypeApproximation
public val FrameColumnTypeApproximation: TypeApproximation get() = TODO()

public class ConvertApproximation(public val schema: PluginDataFrameSchema, public val columns: List<List<String>>)

public class Add : AbstractSchemaModificationInterpreter() {
    public val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    public val Arguments.name: String by string()
    public val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(receiver.columns() + SimpleCol(name, type))
    }
}

public class Add1 : AbstractSchemaModificationInterpreter() {

    public val Arguments.name: String by string()
    public val Arguments.expression: TypeApproximation by type()
    public val Arguments.parent: String by string()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(listOf(SimpleCol(name, expression)))
    }
}

public class From : AbstractInterpreter<Unit>() {
    public val Arguments.dsl: AddDslApproximation by arg(lens = Interpreter.Value)
    public val Arguments.receiver: String by string()
    public val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret() {
        dsl.columns += SimpleCol(receiver, type)
    }
}

public class Into : AbstractInterpreter<Unit>() {
    public val Arguments.dsl: AddDslApproximation by arg(lens = Interpreter.Value)
    public val Arguments.receiver: TypeApproximation by type()
    public val Arguments.name: String by string()

    override fun Arguments.interpret() {
        dsl.columns += SimpleCol(name, receiver)
    }
}


public class AddDslApproximation(public val columns: MutableList<SimpleCol>)

public class AddWithDsl : AbstractSchemaModificationInterpreter() {
    public val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    public val Arguments.body: (Any) -> Unit by arg(lens = Interpreter.Dsl)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val addDsl = AddDslApproximation(receiver.columns().toMutableList())
        body(addDsl)
        return PluginDataFrameSchema(addDsl.columns)
    }
}

public sealed interface AnalysisResult {
    public class New(public val properties: List<Property>) : AnalysisResult {
        public operator fun plus(other: New): New {
            return New(properties + other.properties)
        }
    }

    public class Update(
        public val parent: String,
        public val updatedProperties: List<Property>,
        public val newProperties: List<Property>
    ) : AnalysisResult
}

public data class Property(val name: String, val type: String)
