#!/usr/bin/env sh
# Génère une paire de clés RSA pour signer les jetons d'administration.
set -e
DIR="${1:-src/main/resources}"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DIR/privateKey.pem"
openssl rsa -pubout -in "$DIR/privateKey.pem" -out "$DIR/publicKey.pem"
echo "Clés générées dans $DIR"
