/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element could be null under some circumstances.
 * <p>
 * In general, this means developers will have to read the documentation to
 * determine when a null value is acceptable and whether it is necessary to
 * check for a null value.
 * <p>
 * This annotation is useful mostly for overriding a {@link Nonnull} annotation.
 * Static analysis tools should generally treat the annotated items as though they
 * had no annotation, unless they are configured to minimize false negatives.
 * <p>
 * When this annotation is applied to a method it applies to the method return value.
 *
 * @see jakarta.annotation.Nonnull
 * @since 2.0
 */
@Documented
@Retention(RUNTIME)
public @interface Nullable {
}
