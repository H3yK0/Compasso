# Onde o app é publicado (e por quê)

Situação em 01/11/2026. Todas as cópias do app (GitHub, Obtainium e, no futuro, F-Droid) são o mesmo APK, assinado
com a mesma chave (SHA-256 do certificado
`c98ea9ad403d51fbb661afa0e7c1d936c497b1c3ebca76be9488b0f09113356c`). Por isso dá para trocar de fonte sem perder os
dados: o Android aceita a atualização de qualquer uma delas.

| Onde | Situação |
|---|---|
| GitHub Releases | Publicado. Cada versão tem o `Compasso-<versão>.apk` anexado ao release `v<versão>`. |
| Obtainium | Funciona hoje, sem pedido a ninguém: lê os Releases do GitHub. |
| F-Droid | Pedido fechado pela regra dos 3 meses. Reenvio a partir de 01/02/2027. |
| IzzyOnDroid | Não se aplica (política sobre IA generativa). |
| Aptoide | Possível, mas pago e com verificação de identidade. Adiado. |

## GitHub Releases e Obtainium

O [Obtainium](https://github.com/ImranR98/Obtainium) é um app livre que instala e atualiza apps direto das páginas de
release de cada projeto. A pessoa toca em **Add App**, cola `https://github.com/H3yK0/Compasso` e pronto: a cada
release novo com o APK anexado, o Obtainium avisa e atualiza.

O que o Compasso precisa fazer para isso continuar funcionando é o que já faz a cada versão: criar o release
`v<versão>` e anexar **um** APK assinado. Com mais de um APK no mesmo release, o Obtainium pode pedir para a pessoa
escolher, então é melhor manter um só.

O selo "Get it on Obtainium" dos READMEs aponta para o repositório do Obtainium, não para o atalho
`apps.obtainium.imranr.dev/redirect`: essa página de atalho carrega anúncios do Google e analytics, o que não combina
com um app sem rastreamento.

## F-Droid

- O pedido **!55150** (`New app: Compasso`) foi aberto em 31/10/2026. Com a versão 0.0.0.2, o pipeline passou
  inteiro, incluindo o build reproduzível ("compared built binary to supplied reference binary successfully").
- Em 01/11/2026 os revisores fecharam o pedido porque o app "não atende à política que exige manutenção por mais de
  3 meses" (texto do aviso por e-mail). A [política de inclusão](https://f-droid.org/docs/Inclusion_Policy/) diz que o app deve
  estar em desenvolvimento ativo há pelo menos 3 meses, salvo exceções (usuários comprovados, mantenedor com outros
  apps no F-Droid) que não se aplicam aqui. O repositório começou em 31/10/2026.
- **Reenvio a partir de 01/02/2027**, com o passo a passo em [`fdroid/README.md`](../fdroid/README.md). Até lá,
  manter commits e releases regulares: é isso que mostra o "desenvolvimento ativo".
- A política de inclusão do F-Droid não tem regra sobre código escrito com IA.

## IzzyOnDroid

A [política de inclusão](https://izzyondroid.org/docs/general/AppInclusionPolicy/) do IzzyOnDroid diz: "We are
strongly opposed to apps which are fully or in part created by generative AI tools" e "Vibe-coded apps will be
rejected". O código do Compasso foi escrito com ajuda de IA, e os commits dizem isso abertamente
(`Co-Authored-By: Claude`). Por isso o app não foi enviado: seria recusado, e omitir isso iria contra a regra de
transparência deles.

## Aptoide

Pelo [Aptoide Connect](https://docs.connect.aptoide.com/) (antigo Catappult), que também distribui para outras lojas
parceiras e fabricantes:

- **Custo:** assinatura anual de 69 USD (ou 69 EUR), renovação automática. Os apps só são revisados com a
  assinatura ativa.
- **Prova de que o app é seu:** como o app não está no Google Play, é preciso preencher o "Application Ownership
  Request", com dados sobre você ou sua empresa e sobre o app.
- **Identidade:** o Aptoide pede a verificação de desenvolvedor do Google (documento oficial) e o registro do pacote
  `io.github.h3yk0.compasso`. Há também um formulário de conformidade com dados seus e das práticas de privacidade. O
  nome não aparece para o público, mas o Google e o Aptoide ficam com seus dados reais.
- **Passos:** criar a conta, assinar, **Add App** → Android → "Is your app in Google Play?" **No** → pacote
  `io.github.h3yk0.compasso` → textos, ícone e capturas (os de `fastlane/metadata/android/` servem) → APK assinado
  com a mesma chave → pedido de propriedade → revisão manual.
- Não conferi as regras de conteúdo deles para um app sobre saúde sexual.

Adiado em 01/11/2026 por causa do custo e dos dados pessoais.

## Verificação de desenvolvedor do Google

- Desde **30/09/2026**, no Brasil, na Indonésia, em Singapura e na Tailândia, aparelhos Android certificados só
  instalam apps de desenvolvedores verificados **quando vêm das lojas participantes** (o Google Play e lojas de
  fabricantes). Instalar pelo GitHub, pelo Obtainium ou pelo F-Droid não mudou.
- O Google prevê ampliar a regra para todos os apps em aparelhos certificados em **2027**, sem data por país. A
  partir daí, um app sem registro só instalaria pelo "fluxo avançado" (modo desenvolvedor, reiniciar, esperar 24 h e
  confirmar) ou por ADB. Há ainda contas de "distribuição limitada", sem documento e sem taxa, mas só para até 20
  aparelhos.
- O F-Droid recomenda aos desenvolvedores não se registrarem. Nenhuma ação agora: acompanhar quando o Google
  anunciar a data da ampliação no Brasil.

Fontes: [Android developer verification](https://developer.android.com/developer-verification),
[F-Droid 2.0 and Google's Android developer verification](https://anoni.net/news/en/2026/10/fdroid-2-android-developer-verification/).
