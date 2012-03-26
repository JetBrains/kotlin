package org.jetbrains.kotlin.doc

import kotlin.*
import kotlin.util.*
import java.util.*
import org.jetbrains.kotlin.doc.model.KPackage

/**
 * The configuration used with KDoc
 */
class KDocConfig() {

    /**
     * Returns the name of the documentation set
     */
    public var title: String = "Documentation"

    /**
     * Returns the version name of the documentation set
     */
    public var version: String = "SNAPSHOT"

    /**
     * Returns a map of the package prefix to the URLs to use to link to it in the documentation
     */
    public val packagePrefixToUrls: Map<String, String> = TreeMap<String, String>(LongestFirstStringComparator())

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    public val ignorePackages: Set<String> = HashSet<String>()

    /**
    * Returns true if a warning should be generated if there are no comments
    * on documented function or property
    */
    public var warnNoComments: Boolean = true

    /**
    * Returns true if protected functions and properties should be documented
    */
    public var includeProtected: Boolean = true

    {
        // add some common defaults
        addPackageLink("http://docs.oracle.com/javase/6/docs/api/", "java", "org.w3c.dom", "org.xml.sax", "org.omg", "org.ietf.jgss")
        addPackageLink("http://kentbeck.github.com/junit/javadoc/latest/", "org.junit", "junit")
    }

    /**
     * Returns a set of all the package which have been warned that were missing an external URL
     */
    public val missingPackageUrls: Set<String> = TreeSet<String>()

    /**
     * Adds one or more package prefixes to the given javadoc URL
     */
    fun addPackageLink(url: String, vararg packagePrefixes: String): Unit {
        for (p in packagePrefixes) {
            packagePrefixToUrls.put(p, url)
        }
    }

    /**
     * Resolves a link to the given class name
     */
    fun resolveLink(packageName: String): String {
        // TODO should be able to do something like
        // for (e in packageUrls.filterNotNull()) {
        val entrySet = packagePrefixToUrls.entrySet()
        if (entrySet != null) {
            for (e in entrySet) {
                val p = e?.getKey()
                val url = e?.getValue()
                if (p != null && url != null) {
                    if (packageName.startsWith(p)) {
                        return url
                    }
                }
            }
        }
        if (missingPackageUrls.add(packageName)) {
            println("Warning: could not find external link to package: $packageName")
        }
        return ""
    }

    /**
     * Returns true if the package should be included in the report
     */
    fun includePackage(pkg: KPackage): Boolean {
        return !ignorePackages.any{ pkg.name.startsWith(it) }
    }
}

protected class LongestFirstStringComparator() : Comparator<String> {
    override fun compare(s1: String?, s2: String?): Int {
        return compareBy(s1, s2, { (s: String) -> s.length() })
    }

    override fun equals(obj : Any?) : Boolean {
        return obj is LongestFirstStringComparator
    }
}