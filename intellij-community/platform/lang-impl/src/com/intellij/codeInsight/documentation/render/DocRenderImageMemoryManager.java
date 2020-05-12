// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.diagnostic.VMOptions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import sun.awt.image.*;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;

class DocRenderImageMemoryManager {
  private static final Logger LOG = Logger.getInstance(DocRenderImageMemoryManager.class);

  private static final LinkedHashMap<Image, Integer> CACHE_NON_PAINTED = new LinkedHashMap<>();
  private static final LinkedHashMap<Image, Integer> CACHE_PAINTED = new LinkedHashMap<>();
  private static final int CACHE_SIZE_LIMIT_KB;
  private static int TOTAL_SIZE;

  static {
    int memorySizeMb = 750; // default value, if something goes wrong
    try {
      memorySizeMb = VMOptions.readOption(VMOptions.MemoryKind.HEAP, true);
    }
    catch (Throwable e) {
      LOG.error("Failed to get Xmx", e);
    }
    int cacheSize = 1000; // default value, if something goes wrong
    try {
      cacheSize = Math.max(cacheSize, (int) (memorySizeMb * 1024 * Registry.get("doc.render.image.cache.size").asDouble()));
    }
    catch (Throwable e) {
      LOG.error("Error calculating cache size limit", e);
    }
    CACHE_SIZE_LIMIT_KB = cacheSize;
    LOG.debug("Cache size: " + CACHE_SIZE_LIMIT_KB + "kB");
  }

  private synchronized static void register(@NotNull Image image, int size) {
    unregister(image);
    CACHE_NON_PAINTED.put(image, size);
    TOTAL_SIZE += size;

    // trim the cache
    while (CACHE_NON_PAINTED.size() > 1 /* don't remove just registered image */ && TOTAL_SIZE > CACHE_SIZE_LIMIT_KB) {
      Image toRemove = CACHE_NON_PAINTED.keySet().iterator().next();
      toRemove.flush();
    }
    while (!CACHE_PAINTED.isEmpty() && TOTAL_SIZE > CACHE_SIZE_LIMIT_KB) {
      Image toRemove = CACHE_PAINTED.keySet().iterator().next();
      toRemove.flush();
    }
  }

  private synchronized static void unregister(@NotNull Image image) {
    Integer oldSize = CACHE_NON_PAINTED.remove(image);
    if (oldSize != null) TOTAL_SIZE -= oldSize;
    oldSize = CACHE_PAINTED.remove(image);
    if (oldSize != null) TOTAL_SIZE -= oldSize;
  }

  synchronized static void notifyPainted(@NotNull Image image) {
    Integer size = CACHE_NON_PAINTED.remove(image);
    if (size != null) {
      CACHE_PAINTED.put(image, size);
    }
  }

  static void dispose(@NotNull Image image) {
    if (image instanceof ManagedImage) ((ManagedImage)image).dispose();
    image.flush();
  }

  private static class ManagedImage extends ToolkitImage implements ImageConsumer {
    private int myWidth;
    private int myHeight;

    private ManagedImage(URL url) {
      super(new CachingImageSource(url));
      getSource().addConsumer(this);
    }

    private void dispose() {
      getSource().removeConsumer(this);
    }

    @Override
    public void setDimensions(int width, int height) {
      myWidth = width;
      myHeight = height;
    }

    @Override
    public void imageComplete(int status) {
      if (myWidth > 0 && myHeight > 0) {
        register(this, myWidth * myHeight >>> 8 /* assuming 4 bytes per pixel */);
      }
    }

    @Override
    public void flush() {
      super.flush();
      unregister(this);
    }

    @Override
    public void setProperties(Hashtable<?, ?> props) {}

    @Override
    public void setColorModel(ColorModel model) {}

    @Override
    public void setHints(int hintflags) {}

    @Override
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {}

    @Override
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {}
  }

  private static class CachingImageSource extends FileImageSource {
    private final URL myURL;

    private CachingImageSource(URL url) {
      super(null);
      myURL = url;
    }

    @Override
    protected ImageDecoder getDecoder() {
      InputStream stream = CachingDataReader.getInstance().getInputStream(myURL);
      return stream == null ? null : getDecoder(stream);
    }
  }

  static final Dictionary<URL, Image> IMAGE_SUPPLIER = new Dictionary<URL, Image>() {
    @Override
    public Image get(Object key) {
      if (!(key instanceof URL)) return null;
      return new ManagedImage((URL)key);
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<URL> keys() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<Image> elements() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Image put(URL key, Image value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Image remove(Object key) {
      throw new UnsupportedOperationException();
    }
  };
}
