// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Date;

/**
 * Abstract interface to a source file.
 */
public interface Source {

  /**
   * Determines whether the given source exists.
   */
  boolean exists();

  /**
   * Returns the last-modified timestamp for this source, using the same units as
   * {@link Date#getTime()}.
   */
  long getLastModified();

  /**
   * Gets the name of this source.
   */
  String getName();

  /**
   * Gets a reader for the dart file's source code. The caller is responsible for closing the
   * returned reader.
   */
  Reader getSourceReader() throws IOException;

  /**
   * Gets the identifier for this source. This is used to uniquely identify the
   * source, but should not be used to obtain the source content. Use
   * {@link #getSourceReader()} to obtain the source content.
   */
  URI getUri();
}
