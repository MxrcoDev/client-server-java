#!/bin/bash
# -------------------------------
# Script per compilare e avviare Server e Client in Bash
# -------------------------------

echo "Compilazione dei file Java..."
javac *.java

# Controlla se la compilazione è andata a buon fine
if [ $? -ne 0 ]; then
    echo "Errore nella compilazione. Uscita."
    exit 1
fi

echo "Compilazione completata."

read -p "Quanti client vuoi aprire? " n

# Avvia il server in background
echo "Avvio del server..."
java Server &
SERVER_PID=$!
sleep 1  # attesa di 1 secondo per sicurezza

# Avvia i client in background
echo "Avvio dei client..."
for ((i=1; i<=n; i++)); do
    java Client &
done

echo "Tutti i processi avviati. Premi Ctrl+C per terminare."

# Mantieni lo script in esecuzione finché i processi non finiscono
wait
