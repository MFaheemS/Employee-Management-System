@echo off
echo ============================================
echo  Employee Management System - Run
echo ============================================

set JAVAFX_LIB=javafx-sdk-25.0.2\lib
set OUT=out

java --module-path "%JAVAFX_LIB%" ^
     --add-modules javafx.controls,javafx.fxml ^
     -cp %OUT% ^
     com.ems.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to run. Make sure you compiled first with "compile.bat".
    pause
)
