@echo off
echo ============================================
echo  Employee Management System - Compile
echo ============================================

set JAVAFX_LIB=javafx-sdk-25.0.2\lib
set SQLITE_VERSION=3.46.1.3
set SQLITE_JAR=lib\sqlite-jdbc-%SQLITE_VERSION%.jar
set SQLITE_URL=https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/%SQLITE_VERSION%/sqlite-jdbc-%SQLITE_VERSION%.jar
set SRC=src\com\ems
set OUT=out

if not exist lib mkdir lib

if not exist "%SQLITE_JAR%" (
    echo Downloading SQLite JDBC driver...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -Uri '%SQLITE_URL%' -OutFile '%SQLITE_JAR%' -UseBasicParsing } catch { exit 1 }"

    if %ERRORLEVEL% neq 0 (
        echo.
        echo [ERROR] Could not download SQLite JDBC driver.
        echo Check your internet connection and rerun compile.bat.
        pause
        exit /b 1
    )
)

:: Create output directory
if not exist %OUT%\com\ems mkdir %OUT%\com\ems

echo Compiling Java sources...
javac --module-path "%JAVAFX_LIB%" ^
      --add-modules javafx.controls,javafx.fxml ^
      -cp "%SQLITE_JAR%" ^
      -d %OUT% ^
      %SRC%\*.java

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
