# Publicar no F-Droid

O F-Droid compila o app a partir do código-fonte, no servidor dele, e assina com a chave dele. Não dá para
"subir um APK": o pedido é um merge request no repositório de metadados do F-Droid (`fdroiddata`, no GitLab),
revisado por voluntários. Tudo do lado do código já está pronto neste repositório:

- `io.github.h3yk0.compasso.yml`: a receita de build (licença, categorias, como compilar e como achar versões novas).
- `../fastlane/metadata/android/`: título, descrições, notas da versão, ícone e capturas de tela em pt-BR,
  en-US e es-ES. O F-Droid lê essa pasta direto do repositório, a cada versão.
- O build de release sem assinatura (`./gradlew assembleRelease` sem `keystore.properties`) funciona sem
  internet além do Google Maven e do Maven Central, sem bibliotecas fechadas, sem o bloco de dependências
  do Google e sem o commit do git dentro do APK.

## Verificações já feitas (com o fdroidserver 2.4.5)

- `fdroid lint` e `fdroid rewritemeta`: a receita passa e já está no formato canônico.
- `fdroid build` a partir da tag `v0.0.0.1` (commit `0ef5962`): o scanner do F-Droid não encontrou nenhum
  problema (só removeu o `gradle-wrapper.jar`, como faz com todo app) e o build terminou com sucesso.
- O APK gerado pelo F-Droid é **idêntico, byte a byte**, ao `assembleRelease` local: o build é reproduzível.
- `fdroid checkupdates`: encontra a versão 0.0.0.1 (código 1) pela tag.

## Passo a passo

1. Crie uma conta em https://gitlab.com (pode usar o mesmo apelido, H3yK0).
2. Abra https://gitlab.com/fdroid/fdroiddata e clique em **Fork**.
3. No seu fork, crie um branch chamado `io.github.h3yk0.compasso`.
4. Adicione o arquivo `metadata/io.github.h3yk0.compasso.yml` com o conteúdo de `fdroid/io.github.h3yk0.compasso.yml`
   deste repositório (pelo site do GitLab: **+ → New file**).
5. Faça o commit e abra um **merge request** para `fdroid/fdroiddata`, escolhendo o modelo **App inclusion**
   e marcando os itens da lista (o app é seu, o código é aberto, não tem rastreamento nem anúncios).
6. O pipeline do GitLab vai rodar `fdroid lint` e um build de teste. Se algo falhar, os revisores comentam no
   merge request; normalmente é um ajuste pequeno na receita.
7. Depois do merge, o app aparece no F-Droid em alguns dias (o índice é gerado uma ou duas vezes por semana).

## Pontos que os revisores podem comentar

- **Categorias**: `Habit Tracker` e `Sports & Health` (da lista atual do F-Droid). Se pedirem outra, troque.
- **Aviso de conteúdo**: a lista atual de anti-features do F-Droid não tem mais "NSFW" (o `fdroid lint` recusa),
  por isso a receita não marca nenhuma. O app não tem anúncios, rastreamento, internet nem partes fechadas.
- **Reproducible builds** (opcional): se um dia você quiser publicar o seu próprio APK assinado e ter o
  F-Droid distribuindo exatamente o mesmo arquivo, dá para adicionar `Binaries:` e `AllowedAPKSigningKeys:`
  na receita. Para a primeira versão não precisa.

## Versões novas

1. Aumente `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Escreva as notas em `fastlane/metadata/android/<idioma>/changelogs/<versionCode>.txt`.
3. Faça o commit no `main` e crie a tag `v<versionName>` (por exemplo `v0.0.0.2`).

Com `UpdateCheckMode: Tags` e `AutoUpdateMode: Version`, o F-Droid encontra a tag nova e compila sozinho.
