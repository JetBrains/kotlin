package jet.typeinfo;

import java.util.List;

/**
 * @author alex.tkachman
 */
public interface TypeInfoProjection {
    TypeInfoProjection[] EMPTY_ARRAY = new TypeInfoProjection[0];

    TypeInfoVariance getVariance();

    TypeInfo getType();

}
