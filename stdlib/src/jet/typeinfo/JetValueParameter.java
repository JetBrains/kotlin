package jet.typeinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for parameters
 *
 * @author alex.tkachman
 *
 * @url http://confluence.jetbrains.net/display/JET/Jet+Signatures
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JetValueParameter {
    /**
     * @return name of parameter
     */
    String name ();

    /**
     * @return type projections or empty
     */
    JetTypeProjection[] typeProjections() default {};

    /**
     * @return is this type nullable
     */
    boolean nullable () default false;

    /**
     * @return if this parameter has default value
     */
    boolean hasDefaultValue () default false;

    /**
     * @return type unless Java type is correct Kotlin type.
     */
    String type() default "";
}
