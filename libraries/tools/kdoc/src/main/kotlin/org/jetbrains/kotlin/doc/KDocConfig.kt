package org.jetbrains.kotlin.doc

import java.util.*
import org.jetbrains.kotlin.doc.model.KPackage

/**
* The configuration used with KDoc
*/
class KDocConfig() {

    /**
     * Returns the directory to use to output the API docs
     */
    public var docOutputDir: String = "target/site/apidocs"

    /**
     * Returns the name of the documentation set
     */
    public var title: String = "Documentation"

    /**
     * Returns the version name of the documentation set
     */
    public var version: String = "SNAPSHOT"

    /**
     * Returns a map of the package prefix to the HTML URL for the root of the apidoc using javadoc/kdoc style
     * directory layouts so that this API doc report can link to external packages
     */
    public val packagePrefixToUrls: MutableMap<String, String> = TreeMap<String, String>(LongestFirstStringComparator())

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    public val ignorePackages: MutableSet<String> = HashSet<String>()

    /**
    * Returns true if a warning should be generated if there are no comments
    * on documented function or property
    */
    public var warnNoComments: Boolean = true

    /**
     * Returns the HTTP URL of the root directory of source code that we should link to
     */
    public var sourceRootHref: String? = null

    /**
     * The root project directory used to deduce relative file names when linking to source code
     */
    public var projectRootDir: String? = null

    /**
     * A map of package name to html or markdown files used to describe the package. If none is
     * specified we will look for a package.html or package.md file in the source tree
     */
    public var packageDescriptionFiles: MutableMap<String,String> = HashMap<String,String>()

    /**
     * A map of package name to summary text used in the package overviews
     */
    public var packageSummaryText: MutableMap<String,String> = HashMap<String, String>()

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
    private val mutableMissingPackageUrls: MutableSet<String> = TreeSet<String>()
    public val missingPackageUrls: Set<String> = mutableMissingPackageUrls

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
    fun resolveLink(packageName: String, warn: Boolean = true): String {
        for (e in packagePrefixToUrls) {
            val p = e.key
            val url = e.value
            if (packageName.startsWith(p)) {
                return url
            }
       }
        if (warn && mutableMissingPackageUrls.add(packageName)) {
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

private class LongestFirstStringComparator() : Comparator<String> {
    public override fun compare(s1: String, s2: String): Int {
        return compareBy<String>(s1, s2, { length() }, { this })
    }

    public override fun equals(obj : Any?) : Boolean {
        return obj is LongestFirstStringComparator
    }
}
