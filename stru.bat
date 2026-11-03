@echo off
setlocal
set "pfad=%~dp0"
set "output=%pfad%Ordnerstruktur.txt"
tree "%pfad%" /F /A > "%output%"
