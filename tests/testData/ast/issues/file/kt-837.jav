package com.voltvoodoo.saplo4j.model;

import java.io.Serializable;

public class Language implements Serializable {
        public static Language ENGLISH = new Language("en");
        public static Language SWEDISH = new Language("sv");

        protected String code;
        private static final long serialVersionUID = -2442762969929206780L;

        public Language(String code) {
                this.code = code;
        }

        public boolean equals(Language other) {
                return other.toString().equals(this.toString());
        }
}