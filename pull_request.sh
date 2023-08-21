#!/bin/sh
user="jesyspa"
repo="kotlin"
base="formal-verification"
branch=`git rev-parse --abbrev-ref HEAD`

xdg-open "https://github.com/$user/$repo/compare/$base...$user:$repo:$branch?expand=1"
