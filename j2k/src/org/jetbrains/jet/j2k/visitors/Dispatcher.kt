package org.jetbrains.jet.j2k.visitors

import org.jetbrains.jet.j2k.Converter

public open class Dispatcher(converter: Converter) {
    public var expressionVisitor: ExpressionVisitor = ExpressionVisitor(converter)
}
