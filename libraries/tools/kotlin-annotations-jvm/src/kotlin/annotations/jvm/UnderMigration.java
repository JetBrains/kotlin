/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.annotations.jvm;

import java.lang.annotation.*;

/**
 * This meta-annotation is intended for user nullability annotations with JSR-305 type qualifiers. Behaviour of meta-annotated
 * nullability annotations can be controlled via compilation flag.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface UnderMigration {
    MigrationStatus status();
}
