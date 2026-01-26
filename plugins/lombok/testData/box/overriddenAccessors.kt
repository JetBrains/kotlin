// IGNORE_BACKEND_K1: ANY

// FILE: OverriddenFluent.java

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class OverriddenFluent {
    @Getter @Accessors private int overrideGetterWoFluent = 1;

    @Getter @Accessors(fluent = true) private int overrideGetterWithFluentTrue = 2;

    @Getter @Accessors(chain = true) private int overrideGetterWithUnrelated = 3;

    @Getter @Accessors(fluent = false) private int overrideGetterWithFluentFalse = 4;

    @Setter @Accessors private int overrideSetterWoFluent = 5;

    @Setter @Accessors(fluent = true) private int overrideSetterWithFluentTrue = 6;

    @Setter @Accessors(chain = true) private int overrideSetterWithUnrelated = 7;

    @Setter @Accessors(fluent = false) private int overrideSetterWithFluentFalse = 8;

    void testGetters() {
        overrideGetterWoFluent();
        overrideGetterWithFluentTrue();
        overrideGetterWithUnrelated();

        getOverrideGetterWithFluentFalse();
    }

    void testSetters() {
        overrideSetterWoFluent(5)
            .overrideSetterWithFluentTrue(6)
            .overrideSetterWithUnrelated(7)
            .setOverrideSetterWithFluentFalse(8);
    }
}

// FILE: OverriddenPrefix.java

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true, prefix = { "prefix" })
public class OverriddenPrefix {
    @Accessors private int prefixNoOverride = 1;

    @Accessors(prefix = { "" }) private int overrideWithEmptyPrefix = 2;

    @Accessors(prefix = { "" }) private boolean isHuman;

    @Accessors(prefix = { "" }) private Boolean isNonPrimitiveHuman;

    void test() {
        noOverride();
        overrideWithEmptyPrefix();

        noOverride(1);
        overrideWithEmptyPrefix(2);

        isHuman();
        isHuman(true);

        isNonPrimitiveHuman();
        isNonPrimitiveHuman(true);
    }
}

// FILE: OverriddenChain.java

import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class OverriddenChain {
    @Setter @Accessors private int overrideSetterWoChain = 1;

    @Setter @Accessors(chain = true) private int overrideSetterWithChainTrue = 2;

    @Setter @Accessors(fluent = true) private int overrideSetterWithUnrelated = 3;

    @Setter @Accessors(chain = false) private int overrideSetterWithChainFalse = 4;

    void test() {
        setOverrideSetterWoChain(5)
            .overrideSetterWithUnrelated(6)
            .setOverrideSetterWithChainTrue(7);

        setOverrideSetterWithChainFalse(8);
    }
}

// FILE: test.kt

fun testOverriddenFluent() {
    val overriddenFluent = OverriddenFluent().apply {
        testGetters()
        testSetters()

        overrideGetterWoFluent()
        overrideGetterWithFluentTrue()
        overrideGetterWithUnrelated()

        getOverrideGetterWithFluentFalse()
    }

    overriddenFluent
        .overrideSetterWoFluent(5)
        .overrideSetterWithFluentTrue(6)
        .overrideSetterWithUnrelated(7)
        .setOverrideSetterWithFluentFalse(8)
}

fun testOverriddenPrefix() {
    OverriddenPrefix().apply {
        test()

        noOverride()
        overrideWithEmptyPrefix()

        noOverride(1)
        overrideWithEmptyPrefix(2)

        isHuman()
        isHuman(true)

        isNonPrimitiveHuman()
        isNonPrimitiveHuman(true)
    }
}

fun testOverriddenChain() {
    val overridenChain = OverriddenChain()
    overridenChain.setOverrideSetterWoChain(5)
        .overrideSetterWithUnrelated(6)
        .setOverrideSetterWithChainTrue(7);

    val result: Unit = overridenChain.setOverrideSetterWithChainFalse(8);
}

fun box(): String {
    testOverriddenFluent()
    testOverriddenPrefix()
    testOverriddenChain()
    return "OK"
}
