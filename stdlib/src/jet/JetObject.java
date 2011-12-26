package jet;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public interface JetObject {
    TypeInfo<?> getTypeInfo();
    JetObject getOuterObject();
}
