package org.jetbrains.k2js.translate.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author Talanov Pavel
 */
@SuppressWarnings("NullableProblems")
public final class IsStatement {

    private IsStatement() {
    }

    private static IsStatementVisitor visitor = new IsStatementVisitor();

    public static boolean isStatement(@NotNull JetExpression expression) {
        return (expression.accept(visitor, null));
    }

    private static final class IsStatementVisitor extends JetVisitor<Boolean, Void> {

        @NotNull
        private Boolean visitParent(@NotNull JetElement element) {
            PsiElement parent = element.getParent();
            assert parent != null : "Cannot visit top level expressions.";
            assert parent instanceof JetElement : "Elements of Kotlin PSI must be instances of JetElement";
            return ((JetElement) parent).accept(this, null);
        }

        @Override
        @NotNull
        public Boolean visitJetElement(@NotNull JetElement element, @Nullable Void nothing) {
            return false;
        }

        @Override
        @NotNull
        public Boolean visitBlockExpression(@NotNull JetBlockExpression expression, @Nullable Void nothing) {
            return true;
        }


        @Override
        @NotNull
        public Boolean visitWhenExpression(@NotNull JetWhenExpression expression, @Nullable Void nothing) {
            return visitParent(expression);
        }

        @Override
        @NotNull
        public Boolean visitIfExpression(@NotNull JetIfExpression expression, @Nullable Void nothing) {
            return visitParent(expression);
        }

        @Override
        @NotNull
        public Boolean visitWhenEntry(@NotNull JetWhenEntry expression, @Nullable Void nothing) {
            return visitParent(expression);
        }

        @Override
        @NotNull
        public Boolean visitWhenCondition(@NotNull JetWhenCondition expression, @Nullable Void nothing) {
            return visitParent(expression);
        }

        @Override
        @NotNull
        public Boolean visitContainerNode(@NotNull JetContainerNode expression, @Nullable Void nothing) {
            return visitParent(expression);
        }
    }

}
