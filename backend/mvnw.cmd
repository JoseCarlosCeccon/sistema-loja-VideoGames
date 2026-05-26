@echo off
setlocal

set "BASEDIR=%~dp0"
if "%BASEDIR:~-1%"=="\" set "BASEDIR=%BASEDIR:~0,-1%"
set "WRAPPER_JAR=%BASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper JAR nao encontrado em "%WRAPPER_JAR%".
  exit /b 1
)

if exist "%ProgramFiles%\Java\jdk1.8.0_111\bin\javac.exe" (
  set "JAVA_HOME=%ProgramFiles%\Java\jdk1.8.0_111"
)

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)

echo %JAVA_TOOL_OPTIONS% | findstr /C:"javax.net.ssl.trustStoreType" >nul
if errorlevel 1 (
  set "JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Djavax.net.ssl.trustStoreType=Windows-ROOT"
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%BASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
