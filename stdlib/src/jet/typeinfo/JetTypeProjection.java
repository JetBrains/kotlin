package jet.typeinfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author alex.tkachman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JetTypeProjection {
    /**
     * @return variance of the type
     */
    TypeInfoVariance  variance();

    /**
     * @return index of the class in the per class table of JetTypeDescriptor
     */
    int typeDescriptorIndex();
}
