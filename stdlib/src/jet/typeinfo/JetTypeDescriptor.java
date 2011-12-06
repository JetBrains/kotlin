package jet.typeinfo;

/**
 * @author alex.tkachman
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
