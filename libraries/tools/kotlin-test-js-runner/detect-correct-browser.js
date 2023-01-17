const fs = require("fs");
const path = require("path");
const isWsl = require('is-wsl');
const {platform, arch} = process;

function detectArchBinary(binary) {
    if (typeof binary === 'string' || Array.isArray(binary)) {
        return binary;
    }

    const {[arch]: archBinary} = binary;

    if (!archBinary) {
        throw new Error(`${arch} is not supported`);
    }

    return archBinary;
}

// Return location of chrome.exe file for a given Chrome directory (available: "Chrome", "Chrome SxS").
function getChromeExe(chromeDirName) {
    // Only run these checks on win32
    if (process.platform !== 'win32') {
        return null
    }
    let windowsChromeDirectory, i, prefix;
    const suffix = '\\Google\\' + chromeDirName + '\\Application\\chrome.exe';
    const prefixes = [process.env.LOCALAPPDATA, process.env.PROGRAMFILES, process.env['PROGRAMFILES(X86)']];

    for (i = 0; i < prefixes.length; i++) {
        prefix = prefixes[i]
        try {
            windowsChromeDirectory = path.join(prefix, suffix)
            fs.accessSync(windowsChromeDirectory)
            return windowsChromeDirectory
        } catch (e) {
        }
    }

    return windowsChromeDirectory
}

function detectPlatformBinary({[platform]: platformBinary}, {wsl}) {
    if (wsl && isWsl) {
        return detectArchBinary(wsl);
    }

    if (!platformBinary) {
        throw new Error(`${platform} is not supported`);
    }

    return detectArchBinary(platformBinary);
}

const apps = {};

function define(browserName, platforms, wsl) {
    Object.defineProperty(apps, browserName, {
        configurable: true,
        enumerable: true,
        value: {
            bin: detectPlatformBinary(platforms, wsl)
        }
    });
}

define(
    'chrome',
    {
        darwin: 'google chrome',
        win32: getChromeExe("Chrome"),
        linux: ['google-chrome', 'google-chrome-stable']
    },
    {
        wsl: {
            ia32: '/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe',
            x64: ['/mnt/c/Program Files/Google/Chrome/Application/chrome.exe', '/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe']
        }
    }
);

define(
    'chrome canary',
    {
        darwin: 'google chrome canary',
        win32: getChromeExe("Chrome SxS"),
        linux: ['google-chrome-canary', 'google-chrome-unstable']
    },
    {
        wsl: {
            ia32: '/mnt/c/Program Files (x86)/Google/Chrome SxS/Application/chrome.exe',
            x64: ['/mnt/c/Program Files/Google/Chrome SxS/Application/chrome.exe', '/mnt/c/Program Files (x86)/Google/Chrome SxS/Application/chrome.exe']
        }
    }
);

module.exports = apps