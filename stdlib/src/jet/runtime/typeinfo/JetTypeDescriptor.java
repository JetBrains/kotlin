package jet.runtime.typeinfo;

import jet.typeinfo.TypeInfoVariance;

/**
 * @author alex.tkachman
 *
 * @url http://confluence.jetbrains.net/display/JET/Jet+Signatures
 */
public @interface JetTypeDescriptor{
    //
    // case of type parameter
    //
    String varName() default "";
    boolean reified () default true;
    TypeInfoVariance variance() default TypeInfoVariance.INVARIANT;
    int [] upperBounds() default {};

    //
    // case of real type
    //
    Class  javaClass() default Object.class;
    JetTypeProjection [] projections() default {};
}
