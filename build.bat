@echo off
call clean.bat
echo Generating AST file
ast_builder.py
echo Compiling solution
javac *.java