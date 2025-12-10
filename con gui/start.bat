@echo off
REM -------------------------------
REM Script per l'avvio
REM -------------------------------

echo Compilazione dei file Java...
javac *.java

REM Controlla se la compilazione è andata a buon fine
IF %ERRORLEVEL% NEQ 0 (
    echo Errore nella compilazione. Uscita.
    pause
    exit /b 1
)

echo Compilazione completata.

set /p n="Quanti client vuoi aprire? "

REM Avvia il server in background
start /b java Server
REM Attendi 1 secondo per sicurezza
timeout /t 1 /nobreak >nul

REM Avvia i client nella stessa finestra (background)
echo Avvio dei client...
for /l %%i in (1,1,%n%) do (
    start /b java Client
)

REM
echo Tutti i processi avviati. Premi un tasto per terminare.
pause