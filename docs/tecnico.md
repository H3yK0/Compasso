# Documentação técnica

**Compasso** em português, **Lodestar** em inglês e **Astrolabio** em espanhol: o mesmo app, com um nome próprio em
cada idioma. O repositório, o ID (`io.github.h3yk0.compasso`) e o código continuam chamados Compasso.
[Página principal do projeto](../README.md).


Um refúgio para quem quer sair da pornografia: treino de controle pelo método "para e recomeça"
(rodadas de ritmo com batida, pausas totais com respiração guiada e, dependendo do nível e do controle
inteligente, liberação no fim), treino de Kegel para o assoalho pélvico, um lugar seguro para quando a
vontade apertar, leituras com fontes e evolução por constância. App Android, 100% offline: não pede acesso
à internet.

**Versão 0.0.0.3**. Em português, inglês e espanhol.

## O que tem

| Aba | O que faz |
|---|---|
| **Hoje** | O astro (cor = estado da liberação, anel = pontos do dia), níveis num carrossel, um botão para começar, o cartão "Para você" (nível e leitura sugeridos), a semana em estrelas |
| **Refúgio** | Respirar 2 min com 6 trilhas calmas à escolha, treino de **Kegel** e três jogos originais: Encaixe, Dobra e Eco |
| **Evolução** | Nível pessoal (Iniciante → Invicto), check-in do dia, o céu do mês, gráficos, pódio das melhores semanas, recordes |
| **Ler** | 30 artigos curtos (mente e hábitos, ciência, corpo e sexualidade, relações e emoções, apoio), cada um com as fontes no fim; dá para ler com música calma de fundo |

Os **Ajustes** (controle inteligente, modo foco, sons e trilha, privacidade, visual Noite/Dia, idioma,
ajuda, licença) abrem pelo emblema no alto da tela Hoje.

Durante a sessão, o botão **"Não aguentei"** fica sempre à mão (no ritmo, na pausa e na tela de pausa):
um toque encerra ali e registra com honestidade.

Também: tela de boas-vindas e apresentação guiada, "Como usar" sempre disponível, dicas na primeira
visita a cada parte ("ir descobrindo"), codinome anônimo com emblema gerado pelo app.

## Idiomas

O app segue o idioma do celular: **português**, **inglês** ou **espanhol** (outros idiomas caem no inglês).
Também dá para escolher em Ajustes → Idioma. Traduzidos: todas as telas, os 30 artigos (com as mesmas fontes;
no artigo de ajuda, os serviços do Brasil viram linhas de apoio de outros países), datas, horas e números.

- Nome do app: `app_name` em `values/` (Lodestar), `values-pt/` (Compasso) e `values-es/` (Astrolabio); os textos e os
  artigos de cada idioma usam o nome daquele idioma (o `LocalizationTest` confere que não sobrou o nome de outro).
- Textos das telas: `app/src/main/res/values/strings.xml` (inglês, o padrão), `values-pt/` e `values-es/`.
  As três listas precisam ter as mesmas chaves e os mesmos `%1$s`; o `LocalizationTest` confere.
- Artigos: `app/src/main/assets/artigos/pt`, `en` e `es`, com o mesmo `id`, `ordem`, `tema`, `tags` e
  `minutos` em cada idioma (o `ContentTest` confere, junto com o ritmo de leitura e as fontes).
- O codinome guarda só a posição das palavras: "Lobo Sereno 42" vira "Serene Wolf 42" em inglês.

## Visual "Noite"

Céu noturno com estrelas, cartões de vidro, barra de abas flutuante e o **astro**: a esfera com a rosa
dos ventos que também é o ícone do app. Títulos na serifa Cormorant Garamond, textos e números na Inter.
Capas e ilustrações são desenhadas pelo próprio app (`ui/components/Night.kt`), sem imagens de terceiros.
O modo Dia continua disponível em Ajustes → Visual.

## Níveis

| Nível | Rodadas | Ritmo por rodada | Pausa | Velocidade | No fim |
|---|---|---|---|---|---|
| Fácil | 4 | 25–40 s | 10–15 s | 70 → 120 bpm | libera |
| Médio | 7 | 35–60 s | 15–25 s | 65 → 130 bpm | libera |
| Difícil | 10 | 45–90 s | 20–35 s | 60 → 150 bpm | libera |
| Impossível | 12 | 60–100 s | 15–25 s | 80 → 170 bpm | nunca libera |
| Personalizado | você escolhe | | | | libera, sorteio 50% ou nunca |

"Estou perto" corta o ritmo na hora e manda para uma pausa longa (pausa máxima do nível, mínimo 20 s, + 10 s).
Durante a pausa, o mesmo botão soma mais 10 s.

## Controle inteligente

Regras fixas e visíveis (`model/Pacing.kt`):

1. **Intervalo**: depois de uma liberação (ou de um "não aguentei", ou de uma sessão interrompida), a
   próxima só vem depois do intervalo escolhido: 12, 24 (padrão), 48 ou 72 h.
2. **Sem pressa**: sessão começada menos de 2 h depois do fim da anterior é só de controle.
3. **Semana cheia**: com 5 sessões ou mais nos últimos 7 dias, o intervalo dobra.

Com a liberação fechada, a sessão não é cancelada: vira treino de controle. Deixar o controle mais leve
só vale 24 h depois; mais firme vale na hora.

## Modo foco (`model/Focus.kt`)

Ligado por padrão. Antes de cada sessão o app pede para **fixar a tela** (screen pinning do Android:
bloqueia outros apps, notificações e tela dividida, e fecha vídeos em picture-in-picture). Também:

- `HIDE_OVERLAY_WINDOWS` (Android 12+): janelas flutuantes de outros apps somem durante a sessão;
- `resizeableActivity="false"`: sem tela dividida nos celulares;
- sair do app pausa a sessão; mais de 10 s fora, ou a 3ª saída, encerra como **interrompida**;
- sessão aberta quando o app é fechado vira "interrompida" na próxima abertura;
- sem a tela fixada a sessão continua, mas não vale pontos;
- desligar o modo foco só vale 24 h depois.

## Proteções contra burlar as regras

- **Relógio confiável** (`model/Clock.kt`): atrasar a hora do celular não reabre a liberação, não devolve
  pontos e não encurta esperas. O app guarda o último horário que viu e soma só o tempo que de fato passou.
- **Lapsos**: "não aguentei" e sessões interrompidas ficam registrados (só data e tipo) mesmo com o histórico
  desligado ou apagado, porque contam para a sequência e para o Invicto.
- **Senha**: tentativas erradas ficam gravadas (fechar o app não zera) e, da 5ª em diante, pedem uma espera
  que cresce de 30 s até 15 min. Trocar ou remover a senha pede a senha atual. Com senha, a miniatura nos
  recentes fica escondida (Android 13+).
- **Sem backup e sem transferência**: `allowBackup="false"` e regras de extração de dados que excluem tudo
  (no Android 12+ o `allowBackup` sozinho não bloqueia a transferência entre aparelhos).

## Pontos e níveis (`model/Progress.kt`)

Premiam constância, não "fazer mais sessões". Teto de 50 pontos por dia.

| Ação | Pontos | Limite |
|---|---|---|
| Sessão concluída com liberação | 10 | 1 sessão pontuada por dia |
| Sessão concluída sem liberação | 15 | (idem) |
| "Não aguentei" marcado com honestidade | 3 | (idem) |
| Check-in: um dia longe da pornografia | 10 | 1×/dia |
| Respirar até o fim (2 min) | 5 | 3×/dia |
| Jogar pelo menos 3 min | 3 | 2×/dia |
| Ler um artigo até o fim | 5 | 3×/dia, cada artigo uma vez |
| Treino de Kegel completo | 5 | 3×/dia |

Sessões só pontuam com o modo foco ligado e a tela fixada. O nível conta os pontos dos últimos 28 dias:
Bronze 60, Prata 180, Ouro 360, Platina 540, Diamante 720, Invicto 900 (Invicto também pede 14 dias
seguidos de constância e 14 dias sem "não aguentei" nem sessão interrompida). Cada nível libera uma cor
de destaque enquanto a pessoa estiver nele.

## Kegel (`model/Kegel.kt`)

Treino do assoalho pélvico, o músculo que participa da ereção e da ejaculação. Fica no Refúgio e num cartão
"Kegel de hoje" na tela Hoje, e foi pensado para ser feito mais vezes que as sessões: até 3 treinos por dia
rendem pontos (a sessão rende 1 por dia).

- **Permanência** (quanto segurar cada contração): 3, 5, 8 ou 10 s. Começa em 3 s; cada nível libera depois
  de 5 treinos completos no anterior.
- **Tempo** (quanto dura o treino): 2, 4 ou 6 min.
- **Roteiro**: 5 s de preparo → contrai e segura, solta pelo mesmo tempo (quantas couberem no tempo, no mínimo
  5) → 10 contrações rápidas de 1 s. O astro encolhe ao contrair; a vibração marca cada troca (dá para treinar
  de olhos fechados) e a trilha do Respirar pode tocar junto.
- **Lembretes**: ao abrir o app, "Hora do Kegel" quando ainda falta treino no dia (no máximo a cada 3 h, só de
  dia). Opcionalmente, **notificações no celular** em até 3 horários escolhidos: desligadas por padrão, a
  permissão só é pedida quando a pessoa liga, só avisam se ainda faltar Kegel no dia e o texto não fala de
  Kegel ("Compasso · Um minuto para você hoje."); na tela de bloqueio aparece só "Compasso". Usam alarmes não
  exatos (`AlarmManager.setWindow`) e são reagendadas ao reiniciar o celular. Nada sai do aparelho.
- "Como fazer" com os cuidados (não treinar segurando o xixi, respirar normalmente, soltar por completo, parar
  se doer) e um artigo com as fontes.

## Recomendações (`model/Coach.kt`)

Um algoritmo simples, local e explicável lê as sessões, os tropeços e os pontos e encontra os **sinais**
do momento, em ordem de prioridade: "não aguentei" recente, perder cedo na sessão (ou chegar perto muitas
vezes), sessão interrompida, sessões e tropeços mais à noite, semana cheia, pouca constância, boa sequência
e começo. Cada artigo tem `tags` no cabeçalho; a leitura sugerida é a não lida que mais combina com os sinais.
O nível sugerido desce depois de dois "não aguentei" nas três últimas sessões do nível e sobe depois de três
concluídas quase sem "estou perto" e sem tropeço na semana. O motivo sempre aparece junto da sugestão.

Onde aparece: cartão "Para você" na tela Hoje, destaque no topo da aba Ler, no resumo da sessão e numa
**sugestão de leitura ao abrir o app** (no máximo uma a cada 20 h, a primeira só depois de um dia de uso,
nunca durante a sessão nem com o app travado; dá para desligar em Ajustes → Ajuda → Sugestões de leitura).
Nada sai do aparelho.

## Trilhas e sons

Nada de arquivos de áudio de terceiros: as 6 trilhas (`audio/Ambient.kt` e `audio/Soundscapes.kt`) são
sintetizadas ao vivo e acompanham a respiração: **Maré** (acordes lentos, "mar" de ruído filtrado, sinos
pentatônicos), **Chuva leve**, **Floresta noturna**, **Tigelas**, **Céu estrelado** e **Piano calmo**.
A trilha do Respirar também toca nas pausas da sessão; a da leitura toca só com o leitor aberto e para
quando o app sai da frente. Os sons dos jogos (`audio/GameSounds.kt`) são gerados na primeira abertura.

## Compilar

Precisa do JDK 17+ e do Android SDK (plataforma 35).

```
./gradlew assembleRelease      # APK em app/build/outputs/apk/release/ (assinado se houver keystore.properties)
./gradlew testDebugUnitTest    # testes da lógica, das telas e dos fluxos completos
```

ID do app: `io.github.h3yk0.compasso`. Sem `keystore.properties`, o `assembleRelease` gera um APK sem
assinatura (é o que o F-Droid compila). Os APKs publicados são assinados com `fdroid/assinar.sh`, que assina
o APK sem assinatura no formato que o F-Droid confere (builds reproduzíveis; veja `fdroid/README.md`).
**Guarde a chave e a senha fora do repositório público**: atualizações do app precisam da mesma chave;
com outra chave o Android obriga a desinstalar antes (e o histórico se perde).

## F-Droid

O app segue o que o F-Droid pede: código aberto (AGPL v3.0), só bibliotecas abertas do Google Maven e do
Maven Central, sem rastreamento, sem anúncios e sem internet, sem o bloco de dependências do Google no APK
e sem o commit do git dentro dele. O build é **reproduzível**: o F-Droid compila o mesmo código, confere que o
resultado é idêntico ao APK assinado anexado ao release do GitHub e distribui esse APK, com a assinatura do
H3yK0. A descrição, o ícone e as capturas de tela ficam em `fastlane/metadata/android/` (pt-BR, en-US e es-ES),
que o F-Droid lê sozinho a cada versão. A receita, o script de assinatura e o passo a passo estão em `fdroid/`.

## Licença

- **Código:** GNU Affero General Public License v3.0 (`LICENSE`). Copyright (C) 2026 H3yK0 (`NOTICE`).
- **Artigos** (`app/src/main/assets/artigos`, nos três idiomas): Creative Commons BY-SA 4.0.
- **Fonte Inter** (`app/src/main/res/font/inter.ttf`, recortada): SIL Open Font License 1.1
  (`app/src/main/assets/inter-ofl.txt`).
- **Fonte Cormorant Garamond** (`app/src/main/res/font/cormorant_*.ttf`): SIL Open Font License 1.1
  (`app/src/main/assets/cormorant-ofl.txt`).

Quem distribuir o APK, original ou modificado, deve oferecer junto o código-fonte correspondente, sob a mesma licença
(AGPL v3.0). Versões modificadas devem avisar que foram alteradas.
