/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This file was copied from the javax.persistence :
 * https://github.com/eclipse/javax.persistence/blob/master/src/javax/persistence/Entity.java
 */
package javax.persistence;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the class is an entity. This annotation is applied to the
 * entity class.
 *
 * @since Java Persistence 1.0
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Entity {

    /**
     * (Optional) The entity name. Defaults to the unqualified
     * name of the entity class. This name is used to refer to the
     * entity in queries. The name must not be a reserved literal
     * in the Java Persistence query language.
     */
    String name() default "";
}