# RainLang
Welcome to RainLang, the hydrology DSL

# Building
**NOTE: RainLang requires at least java 21**

In the terminal or otherwise, run `build.bat` this builds the interpreter

# Running
**NOTE: Ensure you have built the program first**

You can run with either `run.bat` or `RainLang.bat` which opens RainLang in interpreter mode

On the other hand, in a terminal pointed to this directory, you can run example programs using the following commands:
- `run <file>`
- `RainLang <file>`

Example programs can be found in the `examples` directory. The following programs are provided
- `run examples/basic.txt` - A basic program with 2 connected rivers
- `run examples/dam.txt` - A program that specifies a river system with 5 rivers gated by a Dam, the Dam opens when water reaches a certain threshold
- `run examples/molonglo.txt` - Simulates the example canberrean river system from Assignment 1

# Implicit reports
If a `hydrology_report()` is never explicitly printed in your program. RainLang will implicitly print it for you. Otherwise RainLang will presume your control and only print when specified