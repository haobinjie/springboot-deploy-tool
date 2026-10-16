@echo off
REM ============================================================
REM SpringBoot Deploy Tool - Windows Installer (MSI/EXE) packaging
REM Prerequisites:
REM   1) JDK 17+ (with jpackage) and Maven on PATH
REM   2) WiX Toolset v3 (candle.exe / light.exe) - REQUIRED by
REM      jpackage to build MSI/EXE installers. Download from:
REM      https://github.com/wixtoolset/wix3/releases
REM      (must be v3.x, NOT v4/v5/v6). Its bin must be on PATH
REM      or installed to the default location.
REM Output: .msi and .exe installers under target\dist-installer
REM   - start-menu / desktop shortcut, uninstall entry, dir chooser
REM NOTE: keep this file ASCII-only to avoid cmd/GBK parsing issues.
REM ============================================================
setlocal

set APP_NAME=SpringBootDeployTool
set APP_VERSION=1.0.0
set MAIN_JAR_NAME=springboot-deploy-tool.jar
set MAIN_JAR=target\%MAIN_JAR_NAME%
set INPUT_DIR=target\dist-input
set OUTPUT_DIR=target\dist-installer

REM ---------- precheck: jpackage ----------
where jpackage >nul 2>nul
if errorlevel 1 (
  echo [ERROR] jpackage not found. Install JDK 17+ and add its bin to PATH.
  goto :error
)

REM ---------- precheck: WiX Toolset v3 ----------
where light >nul 2>nul
if not errorlevel 1 goto :wix_ok
if exist "C:\Program Files (x86)\WiX Toolset v3.11\bin\light.exe" goto :wix_ok
if exist "C:\Program Files (x86)\WiX Toolset v3.14\bin\light.exe" goto :wix_ok
echo [ERROR] WiX Toolset v3 (light.exe / candle.exe) not detected.
echo         jpackage needs WiX v3 to build MSI/EXE installers.
echo         Download from https://github.com/wixtoolset/wix3/releases
echo         and retry. It must be v3.x, not v4/v5/v6.
goto :error
:wix_ok

echo [1/4] Maven package fat-jar ...
call mvn -q clean package
if errorlevel 1 goto :error

echo [2/4] Prepare input dir ...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy "%MAIN_JAR%" "%INPUT_DIR%\" >nul

echo [3/4] jpackage build MSI installer ...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"
jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "deploytool" ^
  --description "Windows visual SpringBoot build-deploy tool" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR_NAME%" ^
  --main-class com.deploytool.Launcher ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-upgrade-uuid "6f1c2b3a-4d5e-4f60-9a7b-1c2d3e4f5a60" ^
  --dest "%OUTPUT_DIR%"
if errorlevel 1 goto :error

echo [4/4] jpackage build EXE installer ...
jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "deploytool" ^
  --description "Windows visual SpringBoot build-deploy tool" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR_NAME%" ^
  --main-class com.deploytool.Launcher ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-upgrade-uuid "6f1c2b3a-4d5e-4f60-9a7b-1c2d3e4f5a60" ^
  --dest "%OUTPUT_DIR%"
if errorlevel 1 goto :error

echo DONE. Installers are in %OUTPUT_DIR%\
dir /b "%OUTPUT_DIR%\*.msi" "%OUTPUT_DIR%\*.exe"
endlocal
exit /b 0

:error
echo Build failed. Check JDK / Maven / WiX environment and the log above.
endlocal
exit /b 1
