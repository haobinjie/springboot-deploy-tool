@echo off
REM ============================================================
REM SpringBoot Deploy Tool - Windows exe 打包脚本
REM 前置：本机安装 JDK 17+（含 jpackage）与 Maven
REM 产物：target\dist\SpringBootDeployTool 目录下的 exe（自带 JRE）
REM ============================================================
setlocal

set APP_NAME=SpringBootDeployTool
set APP_VERSION=1.0.0
set MAIN_JAR_NAME=springboot-deploy-tool.jar
set MAIN_JAR=target\%MAIN_JAR_NAME%
set INPUT_DIR=target\dist-input
set OUTPUT_DIR=target\dist

echo [1/3] Maven 打包 fat-jar ...
call mvn -q clean package
if errorlevel 1 goto :error

echo [2/3] 准备输入目录 ...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy "%MAIN_JAR%" "%INPUT_DIR%\" >nul

echo [3/3] jpackage 生成 exe ...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR_NAME%" ^
  --main-class com.deploytool.Launcher ^
  --dest "%OUTPUT_DIR%"

if errorlevel 1 goto :error

echo 完成！产物位于 %OUTPUT_DIR%\%APP_NAME%
endlocal
exit /b 0

:error
echo 打包失败，请检查 JDK/Maven 环境与错误日志。
endlocal
exit /b 1
