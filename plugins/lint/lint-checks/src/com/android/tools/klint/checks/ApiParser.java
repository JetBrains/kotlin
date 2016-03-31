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


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for the simplified XML API format version 1.
 */
public class ApiParser extends DefaultHandler {

    private static final String NODE_API = "api";
    private static final String NODE_CLASS = "class";
    private static final String NODE_FIELD = "field";
    private static final String NODE_METHOD = "method";
    private static final String NODE_EXTENDS = "extends";
    private static final String NODE_IMPLEMENTS = "implements";

    private static final String ATTR_NAME = "name";
    private static final String ATTR_SINCE = "since";

    private final Map<String, ApiClass> mClasses = new HashMap<String, ApiClass>();

    private ApiClass mCurrentClass;

    ApiParser() {
    }

    Map<String, ApiClass> getClasses() {
        return mClasses;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

        if (localName == null || localName.isEmpty()) {
            localName = qName;
        }

        try {
            if (NODE_API.equals(localName)) {
                // do nothing.

            } else if (NODE_CLASS.equals(localName)) {
                String name = attributes.getValue(ATTR_NAME);
                int since = Integer.parseInt(attributes.getValue(ATTR_SINCE));

                mCurrentClass = addClass(name, since);

            } else if (NODE_EXTENDS.equals(localName)) {
                String name = attributes.getValue(ATTR_NAME);
                int since = getSince(attributes);

                mCurrentClass.addSuperClass(name, since);

            } else if (NODE_IMPLEMENTS.equals(localName)) {
                String name = attributes.getValue(ATTR_NAME);
                int since = getSince(attributes);

                mCurrentClass.addInterface(name, since);

            } else if (NODE_METHOD.equals(localName)) {
                String name = attributes.getValue(ATTR_NAME);
                int since = getSince(attributes);

                mCurrentClass.addMethod(name, since);

            } else if (NODE_FIELD.equals(localName)) {
                String name = attributes.getValue(ATTR_NAME);
                int since = getSince(attributes);

                mCurrentClass.addField(name, since);

            }

        } finally {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    private ApiClass addClass(String name, int apiLevel) {
        ApiClass theClass = mClasses.get(name);
        if (theClass == null) {
            theClass = new ApiClass(name, apiLevel);
            mClasses.put(name, theClass);
        }

        return theClass;
    }

    private int getSince(Attributes attributes) {
        int since = mCurrentClass.getSince();
        String sinceAttr = attributes.getValue(ATTR_SINCE);

        if (sinceAttr != null) {
            since = Integer.parseInt(sinceAttr);
        }

        return since;
    }
}
