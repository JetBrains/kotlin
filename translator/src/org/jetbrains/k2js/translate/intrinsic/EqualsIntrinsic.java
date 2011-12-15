package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public abstract class EqualsIntrinsic implements Intrinsic {

    @NotNull
    private Boolean isNegated = false;

    public void setNegated(boolean isNegated) {
        this.isNegated = isNegated;
    }

    public boolean isNegated() {
        return isNegated;
    }
}
