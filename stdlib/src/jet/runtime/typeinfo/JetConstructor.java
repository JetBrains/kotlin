package jet.runtime.typeinfo;

/**
 * @author Stepan Koltsov
 */
public @interface JetConstructor {
    /**
     * @deprecated Some time later all constructors will be visible
     */
    boolean hidden() default false;
}
