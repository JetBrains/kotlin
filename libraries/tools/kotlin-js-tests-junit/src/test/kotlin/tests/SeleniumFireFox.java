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

import org.jetbrains.kotlin.js.qunit.SeleniumQUnit;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

/**
 */
@RunWith(Parameterized.class)
public class SeleniumFireFox extends SeleniumTest {

    static {
        driver = createFirefoxDriver();
        tester = new SeleniumQUnit(driver);
    }

    public static FirefoxDriver createFirefoxDriver() {
        FirefoxProfile profile = new FirefoxProfile();
        FirefoxDriver answer = new FirefoxDriver(profile);
        return answer;
    }

    public SeleniumFireFox(WebElement element, String name) {
        super(element, name);
    }


/*
    @Parameterized.Parameters
    public static List<Object[]> findTestElements() throws IOException, InterruptedException {
        return SeleniumTest.findTestElements();
    }
*/
}
