// FIR_IDENTICAL

// FILE: Base.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(
    builderMethodName = "createBuilderBase",
    buildMethodName = "execute",
    toBuilder = true,
    setterPrefix = "setBase"
)
public class Base {
    private String x;
}

// FILE: Impl.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(
    builderMethodName = "createBuilderImpl",
    buildMethodName = "execute",
    toBuilder = true,
    setterPrefix = "setImpl"
)
public class Impl extends Base {
    private int y;
}

// FILE: test.kt

fun test() {
    val base = Base.createBuilderBase().setBaseX("base").execute()
    val impl = Impl.createBuilderImpl().setBaseX("impl").setImplY(1).execute()
    val baseBuilder: Base.BaseBuilder<*, *> = base.toBuilder()
    val implBuilder: Impl.ImplBuilder<*, *> = impl.toBuilder()

    val baseBuilder2: Base.BaseBuilder<*, *> = baseBuilder.setBaseX("base2")
    val implBuilder2: Impl.ImplBuilder<*, *> = implBuilder.setBaseX("base3")
    val implBuilder3: Impl.ImplBuilder<*, *> = implBuilder2.setImplY(2)
}
