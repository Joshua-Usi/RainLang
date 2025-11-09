@echo off
call clean.bat
rem echo Generating AST file
rem src\ast_builder.py
echo Adding standard library
copy /Y ".\standard_lib.txt" ".\build\"
echo Compiling solution
javac -d build src\*.java