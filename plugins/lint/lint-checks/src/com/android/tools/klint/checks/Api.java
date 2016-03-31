/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;


import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Main entry point for API description.
 *
 * To create the {@link Api}, use {@link #parseApi(File)}
 *
 */
public class Api {

    /**
     * Parses simplified API file.
     * @param apiFile the file to read
     * @return a new ApiInfo
     */
    public static Api parseApi(File apiFile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(apiFile);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            ApiParser apiParser = new ApiParser();
            parser.parse(fileInputStream, apiParser);
            return new Api(apiParser.getClasses());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    private final Map<String, ApiClass> mClasses;

    private Api(Map<String, ApiClass> classes) {
        mClasses = new HashMap<String, ApiClass>(classes);
    }

    ApiClass getClass(String fqcn) {
        return mClasses.get(fqcn);
    }

    Map<String, ApiClass> getClasses() {
        return Collections.unmodifiableMap(mClasses);
    }
}
