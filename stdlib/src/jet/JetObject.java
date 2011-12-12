package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public interface JetObject {
    TypeInfo<?> getTypeInfo();
    JetObject getOuterObject();
}
