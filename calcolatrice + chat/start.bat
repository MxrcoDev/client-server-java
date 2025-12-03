@echo off
REM -------------------------------
REM Script per compilare e avviare Server e Client
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

REM Avvia il server in una nuova finestra
echo Avvio del server...
start "Server" cmd /k java Server

REM Attendi 1 secondo per sicurezza
timeout /t 1 /nobreak >nul

REM Avvia il client nella finestra corrente
echo Avvio dei client...

for /l %%i in (1,1,%n%) do (
    start "Client %%i" cmd /k java Client
)

REM Quando il client chiude, termina lo script
echo Terminazione.
pause
