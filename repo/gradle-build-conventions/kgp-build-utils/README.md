# kgp-build-utils

Utilities specific for the kotlin-gradle-plugin build.

Motivation: We want to avoid gathering all utilities in buildsrc-compat, because it's shared by many projects so any change in
buildsrc-compat means changing the classpath for all projects.
