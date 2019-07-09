
// NOTE: copied for kscript tests

@file:DependsOn("log4j:log4j:1.2.12")
@file:DependsOn("com.offbytwo:docopt:0.6.0.20150202")

// some pointless comment

import org.docopt.Docopt

// test the docopt dependency
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

// instantiate a logger to test the log4j dependency
org.apache.log4j.Logger.getRootLogger()


println("Succeeded!")
