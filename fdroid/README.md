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
- `fdroid build` do commit `a0a2340` (tag `v0.0.0.3`): o scanner não encontrou problemas; o APK sem assinatura do
  F-Droid é idêntico, byte a byte, ao build do commit `edcda21` (o mesmo código do app).
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

## Pedido de inclusão

**Situação:** o pedido **!55150** (`New app: Compasso`) foi aberto em 31/10/2026 e, com a versão 0.0.0.2, o pipeline
passou inteiro, incluindo o build reproduzível. Em 01/11/2026 os revisores o fecharam pela regra dos 3 meses: a
[política de inclusão](https://f-droid.org/docs/Inclusion_Policy/) pede que o app esteja em desenvolvimento ativo há
pelo menos 3 meses, e o repositório começou em 31/10/2026. O reenvio fica para **fevereiro de 2027**. Não apague o
fork nem o branch `io.github.h3yk0.compasso`: eles servem para o reenvio. Outras lojas: veja
[`docs/distribuicao.md`](../docs/distribuicao.md).

### Reenvio (a partir de 01/02/2027)

1. Confira que houve desenvolvimento nesses 3 meses: commits e releases de novembro a janeiro.
2. Atualize a receita desta pasta para a versão mais nova (`commit` do release, `versionName`, `versionCode`,
   `CurrentVersion` e `CurrentVersionCode`) e confira com `fdroid lint` e `fdroid build`, usando o APK do release.
3. No seu fork do `fdroiddata`, branch `io.github.h3yk0.compasso`, abra `metadata/io.github.h3yk0.compasso.yml`
   e use **Replace** para subir a receita nova.
4. No !55150, deixe um comentário pedindo para reabrir:

   > Hi! Compasso has now been in active development for over 3 months (first commit on 2026-10-31, with regular
   > releases since then). The recipe is updated to the latest version and the pipeline is green. Could you please
   > reopen this MR for review? Thank you!

   Se aparecer o botão **Reopen** para você, pode usá-lo. Se ninguém responder em alguns dias, abra um merge request
   novo do mesmo branch, com o mesmo título, e cite o !55150 na descrição.
5. Acompanhe o pipeline e os comentários dos revisores.

### Primeiro envio (como foi feito)

1. **APK no GitHub**: release `v<versão>` com `Compasso-<versão>.apk` anexado (o nome tem que ser exatamente esse).
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
