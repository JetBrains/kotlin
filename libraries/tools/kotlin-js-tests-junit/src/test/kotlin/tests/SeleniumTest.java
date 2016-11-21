/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.jetbrains.kotlin.js.qunit.SeleniumQUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.List;

/**
 */
@RunWith(Parameterized.class)
public class SeleniumTest {

    protected static String testQueryString = "";
    //protected static String testQueryString = "?testNumber=6";

    protected static class HtmlUnitDriverWithExceptionOnFailingStatusCode extends HtmlUnitDriver {
        HtmlUnitDriverWithExceptionOnFailingStatusCode() {
            super(BrowserVersion.CHROME);
            getWebClient().getOptions().setThrowExceptionOnFailingStatusCode(true);
        }
    }

    protected static WebDriver driver = createDriver();

    public static WebDriver createDriver() {
        HtmlUnitDriver answer = new HtmlUnitDriverWithExceptionOnFailingStatusCode();
        answer.setJavascriptEnabled(true);
        return answer;
    }

    protected static SeleniumQUnit tester = new SeleniumQUnit(driver);

    private final WebElement element;

    public SeleniumTest(WebElement element, String name) {
        this.element = element;
    }

    @Test
    public void run() {
        tester.runTest(element);
    }

    @Parameterized.Parameters(name = "{1}")
    public static List<Object[]> findTestElements() throws IOException, InterruptedException {
        String uri = "../kotlin-js-tests/src/test/web/index.html" + testQueryString;
        File file = new File(uri);
        driver.get("file://" + file.getCanonicalPath());
        Thread.sleep(500);
        List<WebElement> tests = tester.findTests();

        List<Object[]> list = new ArrayList<Object[]>();
        for (WebElement test : tests) {
            Object[] args = new Object[] { test, tester.findTestName(test) };
            list.add(args);
        }
        return list;
    }
}
