@echo off
del /s /q out/*.class >nul 2>&1
echo Cleaned up class files
rem echo Generating AST file
rem src\ast_builder.py
echo Adding standard library
copy /Y ".\standard_lib.txt" ".\build\"
echo Compiling solution
javac -d build src\*.java