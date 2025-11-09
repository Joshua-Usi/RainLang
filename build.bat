@echo off
del /s /q out\*.class >nul 2>&1
echo Cleaned up class files

rem echo Generating AST file
rem src\ast_builder.py

echo Adding standard library
if not exist ".\build\" mkdir ".\build\"
copy /Y ".\RainLang\standard_lib.txt" ".\build\" >nul
echo Standard library added

echo Compiling solution
javac -d build src\*.java
