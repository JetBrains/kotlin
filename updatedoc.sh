#!/bin/sh
# generates the API docs and uploads them to github pages
# for help on github pages see: http://pages.github.com/

# TODO can we fail the build if the docs are not created?
ant doc || exit $?

echo "Cloning/pulling latest gh-pages branch"

git clone -b gh-pages git@github.com:JetBrains/kotlin.git gh-pages
cd gh-pages
git pull || exit $?
rm -rf apidoc/stdlib/*
cp -r ../dist/apidoc/* apidoc
git add apidoc || exit $?
git commit -m "latest apidocs" || exit $?
git push || exit $?

echo "Updated github pages for apidocs"
