package jet.typeinfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author alex.tkachman
 *
 * @url http://confluence.jetbrains.net/display/JET/Jet+Signatures
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
