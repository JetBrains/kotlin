## Kotlin Standard Library JS Tests JUnit Runner

This module runs the QUnit JavaScript tests for the Kotlin Standard library inside JUnit using [Selenium WebDriver](http://seleniumhq.org/projects/webdriver/) to run the QUnit code inside any of the drivers available.

This means we can run the tests automatically in any CI server like TeamCity, Jenkins, Hudson and use any WebDriver provider; such as for FireFox / Chrome / IE etc.

By default we use the HtmlUnitDriver so it runs using an in memory HTML / JavaScript engine.