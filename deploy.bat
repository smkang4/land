@echo off
REM 더블클릭 또는 cmd에서: deploy.bat
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy.ps1" %*
