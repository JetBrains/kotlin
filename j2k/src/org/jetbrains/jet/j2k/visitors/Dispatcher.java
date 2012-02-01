package org.jetbrains.jet.j2k.visitors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;

/**
 * @author ignatov
 */
public class Dispatcher {
    private ExpressionVisitor myExpressionVisitor;

    public void setExpressionVisitor(final ExpressionVisitor expressionVisitor) {
        this.myExpressionVisitor = expressionVisitor;
    }

    public Dispatcher(@NotNull Converter converter) {
        myExpressionVisitor = new ExpressionVisitor(converter);
    }

    public ExpressionVisitor getExpressionVisitor() {
        return myExpressionVisitor;
    }
}
