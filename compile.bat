@echo off
echo ============================================
echo  Employee Management System - Compile
echo ============================================

set JAVAFX_LIB=javafx-sdk-25.0.2\lib
set SRC=src\com\ems
set OUT=out

:: Create output directory
if not exist %OUT%\com\ems mkdir %OUT%\com\ems

echo Compiling Java sources...
javac --module-path "%JAVAFX_LIB%" ^
      --add-modules javafx.controls,javafx.fxml ^
      -d %OUT% ^
      %SRC%\Main.java ^
      %SRC%\LoginController.java ^
      %SRC%\EmployeeAddController.java ^
      %SRC%\EmployeeDeactivateController.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Compilation failed. Make sure JDK is installed and on your PATH.
    pause
    exit /b 1
)

echo Copying FXML and CSS resources...
copy /Y %SRC%\*.fxml %OUT%\com\ems\ > nul
copy /Y %SRC%\*.css  %OUT%\com\ems\ > nul

echo.
echo [SUCCESS] Compilation complete. Run "run.bat" to launch the application.
pause
