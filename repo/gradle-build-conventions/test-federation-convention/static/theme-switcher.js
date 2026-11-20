/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const STORAGE_KEY = 'test-federation-theme';
const THEMES = ['auto', 'light', 'dark'];

(function () {
    applyTheme(readTheme());
    document.addEventListener('DOMContentLoaded', function () {
        const buttons = document.querySelectorAll('.theme-switcher button');

        buttons.forEach(function (button) {
            button.addEventListener('click', function () {
                const theme = button.getAttribute('theme');
                writeTheme(theme);
                applyTheme(theme);
                updateButtons(buttons, theme);
            });
        });
        updateButtons(buttons,readTheme() || 'auto');
    });
})();

function applyTheme(theme) {
    if (THEMES.indexOf(theme) === -1) return;
    document.documentElement.classList.remove('dark', 'light');
    if (theme !== 'auto') document.documentElement.classList.add(theme);
}

function readTheme() {
    try {
        return window.localStorage.getItem(STORAGE_KEY);
    } catch (e) {
        return null;
    }
}

function writeTheme(theme) {
    try {
        window.localStorage.setItem(STORAGE_KEY, theme);
    } catch (e) {}
}

function updateButtons(buttons, theme) {
    buttons.forEach(function (button) {
        const active = button.getAttribute('theme') === theme;
        button.classList.toggle('active', active);
        button.setAttribute('aria-pressed', String(active));
    });
}
