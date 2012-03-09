# Developer Notes

## Handy links

 * [follow Kotlin on twitter](http://twitter.com/#!/project_kotlin)

## Editing Kotlin

 * [Kotlin IDEA Plugin](http://hadihariri.com/2012/02/17/the-kotlin-journey-part-i-getting-things-set-up/)
 * [Kotlin TextMate Bundle](https://github.com/k33g/kotlin-textmate-bundle#readme)

## Kommitter links

* [TeamCity CI build](http://teamcity.jetbrains.com/project.html?projectId=project67&tab=projectOverview)

## Regenerating the website

To rebuild and generate the API docs, from a local checkout

    cd libraries
    mvn install

Now to be able to update the website you will need to add this to your **~/.m2/settings.xml**

````xml
<server>
   <id>github-project-site</id>
   <username>git</username>
</server>
````

Now you can deploy the website via:

    cd website
    mvn site:deploy

