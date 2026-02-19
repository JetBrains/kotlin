# escape=`
# Windows is picky about host and container OS versions. Recommondation:
# Use "LTSC" releases for both.
# https://docs.microsoft.com/en-us/virtualization/windowscontainers/deploy-containers/version-compatibility
# https://cloud.google.com/kubernetes-engine/docs/how-to/creating-a-cluster-windows#choose_your_windows_server_node_image
#
# **Important**: vs_buildtools.exe is buggy and can run and **do nothing** silently
# when running inside Docker.
ARG WINDOWS_VERSION=servercore:ltsc2019
FROM mcr.microsoft.com/windows/${WINDOWS_VERSION}

# Restore the default Windows shell for correct batch processing.
SHELL ["cmd", "/S", "/C"]

# Download the Build Tools bootstrapper.
ADD https://aka.ms/vs/16/release/vs_buildtools.exe C:\TEMP\vs_buildtools.exe
ADD https://github.com/microsoft/vswhere/releases/download/3.1.7/vswhere.exe C:\TEMP\vswhere.exe

# Download channel for fixed install.
ADD https://aka.ms/vs/16/release/channel C:\TEMP\VisualStudio.chman

# Install Build Tools with C++ workload.
#   - Documentation for docker installation
#     https://docs.microsoft.com/en-us/visualstudio/install/build-tools-container?view=vs-2019
#   - Documentation on workloads
#     https://docs.microsoft.com/en-us/visualstudio/install/workload-component-id-vs-build-tools?view=vs-2019#c-build-tools
#   - Documentation on flags
#     https://docs.microsoft.com/en-us/visualstudio/install/use-command-line-parameters-to-install-visual-studio?view=vs-2019
RUN start /w C:\TEMP\vs_buildtools.exe --quiet --wait --norestart --nocache `
    --channelUri C:\TEMP\VisualStudio.chman `
    --installChannelUri C:\TEMP\VisualStudio.chman `
    --installPath C:\BuildTools `
    --add Microsoft.Component.MSBuild `
    --add Microsoft.VisualStudio.Component.Roslyn.Compiler `
    --add Microsoft.VisualStudio.Component.TextTemplating `
    --add Microsoft.VisualStudio.Component.VC.CoreBuildTools `
    --add Microsoft.VisualStudio.Component.VC.CoreIde `
    --add Microsoft.VisualStudio.Component.VC.Redist.14.Latest `
    --add Microsoft.VisualStudio.Component.Windows10SDK `
    --add Microsoft.VisualStudio.ComponentGroup.NativeDesktop.Core `
    --add Microsoft.VisualStudio.Component.TestTools.BuildTools `
    --add Microsoft.VisualStudio.Component.VC.ASAN `
    --add Microsoft.VisualStudio.Component.VC.CMake.Project `
    --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 `
    --add Microsoft.VisualStudio.Component.Windows10SDK.19041 `
    --add Microsoft.Component.VC.Runtime.UCRTSDK `
    --add Microsoft.Net.Component.4.6.1.TargetingPack `
    --add Microsoft.Net.Component.4.8.SDK `
    --add Microsoft.VisualStudio.Component.VC.140 `
    --add Microsoft.VisualStudio.Component.VC.ATL `
    --add Microsoft.VisualStudio.Component.VC.ATLMFC `
    --add Microsoft.VisualStudio.Component.VC.CLI.Support `
    --add Microsoft.VisualStudio.Component.VC.Llvm.Clang `
    --add Microsoft.VisualStudio.Component.VC.Llvm.ClangToolset `
    --add Microsoft.VisualStudio.Component.VC.Modules.x86.x64 `
    --add Microsoft.VisualStudio.Component.VC.v141.x86.x64 `
    --add Microsoft.VisualStudio.Component.Windows10SDK.16299 `
    --add Microsoft.VisualStudio.Component.Windows10SDK.17134 `
    --add Microsoft.VisualStudio.Component.Windows10SDK.17763 `
    --add Microsoft.VisualStudio.Component.Windows10SDK.18362 `
    --add Microsoft.VisualStudio.Component.Windows11SDK.22000 `
    --add Microsoft.VisualStudio.ComponentGroup.NativeDesktop.Llvm.Clang `
    --add Microsoft.VisualStudio.Component.CoreBuildTools `
    --add Microsoft.VisualStudio.Component.VC.ATL `
    && powershell -Command "if ($err = dir $Env:TEMP -Filter dd_setup_*_errors.log | where Length -gt 0 | Get-Content) { throw $err }"

RUN start /w C:/TEMP/vswhere.exe -prerelease -latest -products * -property installationPath

# Workaround to build image
# See. https://stackoverflow.com/questions/76470752/chocolatey-installation-in-docker-started-to-fail-restart-due-to-net-framework
ENV chocolateyVersion=1.4.0

# Set the shell to PowerShell
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]

# Install Chocolatey - a package manager for Windows
RUN Set-ExecutionPolicy Bypass -Scope Process -Force;  `
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072;  `
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'));

# Install build dependencies using Chocolatey
RUN choco install cmake --installargs 'ADD_CMAKE_TO_PATH=User' -y; `
    choco install ninja -y; `
    choco install git -y; `
    choco install python3 --version=3.12.0 -y;

WORKDIR C:\\workspace
