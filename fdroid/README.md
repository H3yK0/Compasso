# Publicar no F-Droid

O F-Droid compila o app a partir do código-fonte, no servidor dele. O pedido de inclusão é um merge request no
repositório de metadados do F-Droid (`fdroiddata`, no GitLab), revisado por voluntários.

O Compasso usa **builds reproduzíveis**: o APK assinado com a chave do H3yK0 fica anexado ao release do GitHub;
o F-Droid compila o mesmo código, confere que o resultado é idêntico e distribui o APK com a assinatura do H3yK0.
Assim, quem instalar pelo GitHub ou pelo F-Droid recebe o mesmo app e pode atualizar por qualquer um dos dois.

Nesta pasta:

- `io.github.h3yk0.compasso.yml`: a receita (licença, categorias, commit da versão, onde está o APK assinado e
  qual chave é aceita).
- `assinar.sh`: gera o APK assinado de uma versão no formato que o F-Droid aceita.
- `../fastlane/metadata/android/`: título, descrições, notas da versão, ícone e capturas em pt-BR, en-US e es-ES.
  O F-Droid lê essa pasta direto do repositório.

## Verificações feitas (fdroidserver 2.4.5)

- `fdroid lint` e `fdroid rewritemeta`: a receita passa e está no formato canônico.
- `fdroid build` do commit `f49df9e` (versão 0.0.0.2): o scanner não encontrou problemas; o APK sem assinatura do
  F-Droid é idêntico, byte a byte, ao `assembleRelease` local.
- Com `Binaries` apontando para o APK assinado: "compared built binary to supplied reference binary successfully"
  e "supplied reference binary has allowed signer". O `apksigcopier compare` também confere.
- `fdroid checkupdates`: encontra a versão pela tag (`v<versão>`).

## A chave de assinatura

- Arquivo `compasso-release.jks` (PKCS12), alias `compasso`, certificado `CN=H3yK0, O=Compasso`.
- SHA-256 do certificado (é o `AllowedAPKSigningKeys` da receita):
  `c98ea9ad403d51fbb661afa0e7c1d936c497b1c3ebca76be9488b0f09113356c`
- **Nunca** coloque a chave ou a senha neste repositório (`*.jks`, `keystore/` e `keystore.properties` estão no
  `.gitignore`). Guarde num gerenciador de senhas e numa cópia offline. **Sem ela, nenhuma versão nova sai**:
  o F-Droid só aceita APKs com essa assinatura.

## Envio ao F-Droid: passo a passo

1. **APK no GitHub**: crie o release `v0.0.0.2` (alvo: o commit `f49df9e` do `main`) e anexe `Compasso-0.0.0.2.apk`
   (o nome tem que ser exatamente esse).
2. **Fork** de https://gitlab.com/fdroid/fdroiddata com a sua conta do GitLab. O fork tem que ser público.
3. **Branch** `io.github.h3yk0.compasso`, a partir de `master`, no fork.
4. **Arquivo** `metadata/io.github.h3yk0.compasso.yml` no branch, com o conteúdo de `io.github.h3yk0.compasso.yml`
   desta pasta. Mensagem do commit: `New app: Compasso`.
5. **Merge request** do branch do fork para `fdroid/fdroiddata` · `master`, com o título `New app: Compasso`.
   O modelo de descrição vem preenchido: responda as duas perguntas do topo, marque o checklist, apague a linha
   `Closes rfp#…` (não há pedido anterior) e mantenha `/label ~"New App"`.
6. O pipeline roda o lint e o build. Se o GitLab pedir telefone ou cartão para rodar o pipeline, não preencha:
   deixe um comentário no merge request.
7. Os revisores respondem por comentário. Depois do merge, o app entra no catálogo na próxima publicação.

**Política de "3 meses":** o F-Droid pede que o app esteja em desenvolvimento ativo há pelo menos 3 meses (salvo
exceções). Se o pedido for aberto antes disso, os revisores podem deixá-lo em espera até completar o prazo.

## Versões novas

1. Aumente `versionCode` e `versionName` em `app/build.gradle.kts` e escreva as notas em
   `fastlane/metadata/android/<idioma>/changelogs/<versionCode>.txt` (pelo menos `en-US`).
2. Faça o commit no `main` e crie a tag `v<versionName>` (por exemplo `v0.0.0.2`) com um release no GitHub.
3. A partir da tag, rode `fdroid/assinar.sh <caminho/da/chave.jks>` e anexe o `Compasso-<versão>.apk` gerado
   ao release dessa tag.
4. Pronto: com `UpdateCheckMode: Tags` e `AutoUpdateMode: Version`, o F-Droid encontra a tag, compila, compara
   com o seu APK e publica. Anexe o APK logo depois de criar a tag: sem ele o build do F-Droid falha.

Não assine pelo Gradle (`keystore.properties`) os APKs destinados ao F-Droid: o `assinar.sh` assina só com os
esquemas v2 e v3 e preservando o alinhamento, que é o que permite ao F-Droid conferir que o build é idêntico.
