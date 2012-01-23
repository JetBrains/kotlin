package jet.runtime.typeinfo;

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
public @interface JetTypeParameter {
    /**
     * @return name of parameter
     */
    String name() default "";
}
