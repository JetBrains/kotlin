#!/bin/sh -e
# generates the API docs and uploads them to github pages
# for help on github pages see: http://pages.github.com/

ec() {
    echo "$@" >&2
    "$@"
}

ec cd $(dirname $0)

# TODO can we fail the build if the docs are not created?
ec ant doc

echo "Cloning/pulling latest gh-pages branch"

if [ ! -e gh-pages ]; then
    ec git clone -b gh-pages git@github.com:JetBrains/kotlin.git gh-pages
    ec cd gh-pages
else
    ec cd gh-pages
    ec git checkout gh-pages
    ec git pull --rebase
fi
ec git rm -r apidoc
ec cp -r ../dist/apidoc apidoc
ec git add apidoc
ec git commit -m "latest apidocs"
ec git push

echo "Updated github pages for apidocs"
