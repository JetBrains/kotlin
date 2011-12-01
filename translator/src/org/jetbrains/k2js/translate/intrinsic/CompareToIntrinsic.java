package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;

/**
 * @author Talanov Pavel
 */
public abstract class CompareToIntrinsic implements Intrinsic {

    private JetToken comparisonToken = null;

    @NotNull
    public JetToken getComparisonToken() {
        assert comparisonToken != null : "Should use set token first";
        return comparisonToken;
    }

    public void setComparisonToken(@NotNull JetToken comparisonToken) {
        assert OperatorConventions.COMPARISON_OPERATIONS.contains(comparisonToken)
                : "Should be a comparison operation";
        this.comparisonToken = comparisonToken;
    }
}
