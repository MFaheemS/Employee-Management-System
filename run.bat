@echo off
echo ============================================
echo  Employee Management System - Run
echo ============================================

set JAVAFX_LIB=javafx-sdk-25.0.2\lib
set SQLITE_VERSION=3.46.1.3
set SQLITE_JAR=lib\sqlite-jdbc-%SQLITE_VERSION%.jar
set OUT=out

if not exist "%SQLITE_JAR%" (
    echo.
    echo [ERROR] Missing SQLite JDBC driver: %SQLITE_JAR%
    echo Run compile.bat first to download dependencies.
    pause
    exit /b 1
)

java --module-path "%JAVAFX_LIB%" ^
     --add-modules javafx.controls,javafx.fxml ^
    -cp "%OUT%;%SQLITE_JAR%" ^
     com.ems.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to run. Make sure you compiled first with "compile.bat".
    pause
)
