@echo off
where mvn >nul 2>nul
if %ERRORLEVEL%==0 (mvn %* & exit /b %ERRORLEVEL%)
set "BASE=%~dp0"
if not exist "%BASE%.mvn\apache-maven-3.9.9\bin\mvn.cmd" (
  powershell -NoProfile -Command "$ProgressPreference='SilentlyContinue';Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%BASE%.mvn\maven.zip';Expand-Archive -Path '%BASE%.mvn\maven.zip' -DestinationPath '%BASE%.mvn' -Force;Remove-Item '%BASE%.mvn\maven.zip'"
  if errorlevel 1 exit /b 1
)
call "%BASE%.mvn\apache-maven-3.9.9\bin\mvn.cmd" %*
