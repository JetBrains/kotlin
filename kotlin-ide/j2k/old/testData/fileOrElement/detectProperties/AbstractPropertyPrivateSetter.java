package test;

import kotlinApi.*;

public class KotlinClassAbstractPropertyImpl extends KotlinClassAbstractProperty {
    private boolean myIsVisible;

    @Override
    public boolean isVisible() {
        return myIsVisible;
    }

    private void test() {
        myIsVisible = true;
    }
}