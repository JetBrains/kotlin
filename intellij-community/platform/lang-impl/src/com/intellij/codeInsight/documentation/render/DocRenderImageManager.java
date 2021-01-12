// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import org.jetbrains.annotations.NotNull;
import sun.awt.image.FileImageSource;
import sun.awt.image.ImageDecoder;
import sun.awt.image.ToolkitImage;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

class DocRenderImageManager extends AbstractDocRenderMemoryManager<Image> {
  DocRenderImageManager() {
    super("doc.render.image.cache.size");
  }

  @Override
  void destroy(@NotNull Image image) {
    image.flush();
  }

  void setCompletionListener(@NotNull Image image, @NotNull Runnable runnable) {
    if (image instanceof ManagedImage) {
      ((ManagedImage)image).completionRunnable = runnable;
    }
  }

  void dispose(@NotNull Image image) {
    if (image instanceof ManagedImage) ((ManagedImage)image).dispose();
    image.flush();
  }

  private class ManagedImage extends ToolkitImage implements ImageConsumer {
    private int myWidth;
    private int myHeight;
    private volatile Runnable completionRunnable;

    private ManagedImage(URL url) {
      super(new CachingImageSource(url));
      ((CachingImageSource)getSource()).setPermanentConsumer(this);
    }

    private void dispose() {
      ((CachingImageSource)getSource()).setPermanentConsumer(null);
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

        myWidth = 0;  // InputStreamImageSource (probably due to a bug) reports IMAGEERROR status after reporting STATICIMAGEDONE
        myHeight = 0; // We want to ignore that

        Runnable runnable = completionRunnable;
        if (runnable != null) runnable.run();
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
    private ImageConsumer myPermanentConsumer;

    private CachingImageSource(URL url) {
      super(null);
      myURL = url;
    }

    private synchronized void setPermanentConsumer(ImageConsumer consumer) {
      if (myPermanentConsumer != null) removeConsumer(myPermanentConsumer);
      myPermanentConsumer = consumer;
    }

    private synchronized void addPermanentConsumer() {
      ImageConsumer consumer = myPermanentConsumer;
      if (consumer != null) addConsumer(consumer);
    }

    @Override
    protected ImageDecoder getDecoder() {
      InputStream stream = CachingDataReader.getInstance().getInputStream(myURL);
      return stream == null ? null : getDecoder(stream);
    }

    @Override
    public void doFetch() {
      addPermanentConsumer();
      super.doFetch();
    }
  }

  Dictionary<URL, Image> getImageProvider() {
    return myImageProvider;
  }

  private final Dictionary<URL, Image> myImageProvider = new Dictionary<URL, Image>() {
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
