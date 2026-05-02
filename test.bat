@echo off
echo ============================================
echo  EMS White-Box Test Suite
echo ============================================

set SQLITE_JAR=lib\sqlite-jdbc-3.46.1.3.jar
set JAVAFX_LIB=javafx-sdk-25.0.2\lib
set SRC=src\com\ems
set TEST=test\com\ems
set OUT=out
set TEST_OUT=test-out

if not exist "%SQLITE_JAR%" (
    echo [ERROR] SQLite JAR not found. Run compile.bat first.
    pause
    exit /b 1
)

if not exist %TEST_OUT% mkdir %TEST_OUT%

echo.
echo Compiling main sources...
javac --module-path "%JAVAFX_LIB%" ^
      --add-modules javafx.controls,javafx.fxml ^
      -cp "%SQLITE_JAR%" ^
      -d %OUT% ^
      %SRC%\*.java
if %ERRORLEVEL% neq 0 ( echo [ERROR] Main compile failed. & pause & exit /b 1 )

echo Compiling test sources...
javac -cp "%SQLITE_JAR%;%OUT%" ^
      -d %TEST_OUT% ^
      %TEST%\*.java
if %ERRORLEVEL% neq 0 ( echo [ERROR] Test compile failed. & pause & exit /b 1 )

echo.
echo Running tests...
echo.
java --enable-native-access=ALL-UNNAMED -cp "%SQLITE_JAR%;%OUT%;%TEST_OUT%" com.ems.WhiteBoxTestRunner

if %ERRORLEVEL% equ 0 (
    echo.
    echo [ALL TESTS PASSED]
) else (
    echo.
    echo [SOME TESTS FAILED]
)

pause
