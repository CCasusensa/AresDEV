@echo off
@title AresDEV - V83
set CLASSPATH=.;lib\*
java -Xmx8000m -Dwzpath=wz\ net.server.Server
pause