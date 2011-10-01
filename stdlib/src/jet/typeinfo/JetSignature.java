package jet.typeinfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * @author alex.tkachman
 */
@Retention(RetentionPolicy.RUNTIME)
@interface JetSignature {
    String value();

}
