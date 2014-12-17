package org.jetbrains.kotlin.gradle.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * A parent-last classloader that will try the child classloader first and then the parent.
 * This takes a fair bit of doing because java really prefers parent-first.
 * <p/>
 * For those not familiar with class loading trickery, be wary
 *
 * http://stackoverflow.com/questions/5445511/how-do-i-create-a-parent-last-child-first-classloader-in-java-or-how-to-overr
 */
public class ParentLastURLClassLoader extends ClassLoader {
    private final ChildURLClassLoader childClassLoader;

    public ParentLastURLClassLoader(@NotNull List<URL> classpath, @Nullable ClassLoader parent) {
        super(Thread.currentThread().getContextClassLoader());

        URL[] urls = classpath.toArray(new URL[classpath.size()]);

        childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(parent));
    }

    @Override
    protected synchronized Class<?> loadClass(@NotNull String name, boolean resolve) throws ClassNotFoundException {
        try {
            // first we try to find a class inside the child classloader
            return childClassLoader.findClass(name);
        }
        catch (ClassNotFoundException e) {
            // didn't find it, try the parent
            return super.loadClass(name, resolve);
        }
    }

    /**
     * This class allows me to call findClass on a classloader
     */
    private static class FindClassClassLoader extends ClassLoader {
        public FindClassClassLoader(@Nullable ClassLoader parent) {
            super(parent);
        }

        @NotNull
        @Override
        public Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }

    /**
     * This class delegates (child then parent) for the findClass method for a URLClassLoader.
     * We need this because findClass is protected in URLClassLoader
     */
    public static class ChildURLClassLoader extends URLClassLoader {
        private final FindClassClassLoader realParent;

        public ChildURLClassLoader(@NotNull URL[] urls, @NotNull FindClassClassLoader realParent) {
            super(urls, null);

            this.realParent = realParent;
        }


        @NotNull
        @Override
        public Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }

            try {
                return super.findClass(name);
            }
            catch (ClassNotFoundException e) {
                // if that fails, we ask our real parent classloader to load the class (we give up)
                return realParent.loadClass(name);
            }
        }
    }
}
