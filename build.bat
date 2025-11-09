@echo off
call clean.bat
rem echo Generating AST file
rem src\ast_builder.py
echo Adding standard library
copy "./standard_lib.txt" "./out"
echo Compiling solution
javac -d build src\*.java