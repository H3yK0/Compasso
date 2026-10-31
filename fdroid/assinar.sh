#!/usr/bin/env bash
# Gera o APK assinado de uma versão, no formato que o F-Droid aceita para builds reproduzíveis.
#
# Uso:  fdroid/assinar.sh <caminho/da/chave.jks>
#       (a senha é pedida na hora; ou defina COMPASSO_KS_PASS)
#
# Precisa do JDK 17+ e do Android SDK (ANDROID_HOME) com build-tools. Rode a partir da tag da versão
# (git checkout v<versão>) para o APK sair igual ao que o F-Droid compila.
#
# Por que assim: o F-Droid compila o app sem assinatura e copia a assinatura do seu APK para o dele. Isso só
# funciona se a assinatura não mexer em mais nada do arquivo: por isso assinamos o APK sem assinatura do
# Gradle com o apksigner, só com os esquemas v2 e v3 (o app pede Android 8+, o v1 não é necessário e
# acrescentaria arquivos ao APK) e preservando o alinhamento.
set -euo pipefail

KS="${1:?informe o caminho da chave .jks}"
ALIAS="${COMPASSO_KS_ALIAS:-compasso}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || { echo "Defina ANDROID_HOME (caminho do Android SDK)."; exit 1; }
BT="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"

if [ -f "$ROOT/keystore.properties" ]; then
  echo "Tire o keystore.properties da raiz antes: o APK precisa sair do Gradle sem assinatura."
  exit 1
fi

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' "$ROOT/app/build.gradle.kts")"
OUT="$ROOT/app/build/outputs/apk/release/Compasso-$VERSION.apk"
UNSIGNED="$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"

(cd "$ROOT" && ./gradlew --quiet clean assembleRelease)

if [ -z "${COMPASSO_KS_PASS:-}" ]; then
  read -r -s -p "Senha da chave: " COMPASSO_KS_PASS
  echo
fi
export COMPASSO_KS_PASS

rm -f "$OUT"
"$BT/apksigner" sign \
  --alignment-preserved \
  --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false \
  --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass env:COMPASSO_KS_PASS \
  --out "$OUT" "$UNSIGNED"
rm -f "$OUT.idsig"

"$BT/apksigner" verify --print-certs "$OUT" | grep -E "certificate SHA-256"
echo
echo "Pronto: $OUT"
echo "Anexe esse arquivo, com esse nome, ao release v$VERSION no GitHub."
