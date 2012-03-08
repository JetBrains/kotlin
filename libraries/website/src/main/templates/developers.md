# Developer Notes

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

