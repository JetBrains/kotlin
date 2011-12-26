package jet.typeinfo;

import jet.TypeInfo;

/**
 * @author alex.tkachman
 */
public interface TypeInfoProjection {
    TypeInfoProjection[] EMPTY_ARRAY = new TypeInfoProjection[0];

    TypeInfoVariance getVariance();

    TypeInfo getType();

}
