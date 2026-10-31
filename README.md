# Compasso

Um refúgio para quem quer sair da pornografia: treino de controle pelo método "para e recomeça"
(rodadas de ritmo com batida, pausas totais com respiração guiada e, dependendo do nível e do controle
inteligente, liberação no fim), um lugar seguro para quando a vontade apertar, leituras com fontes e
evolução por constância. App Android, 100% offline: não pede acesso à internet.

**Versão 0.0.0.1** (primeira versão). Em português, inglês e espanhol.

## O que tem

| Aba | O que faz |
|---|---|
| **Hoje** | O astro (cor = estado da liberação, anel = pontos do dia), níveis num carrossel, um botão para começar, a semana em estrelas |
| **Refúgio** | Respirar 2 min com trilha calma e três jogos originais: Encaixe, Dobra e Eco |
| **Evolução** | Nível pessoal (Iniciante → Invicto), check-in do dia, o céu do mês, gráficos, pódio das melhores semanas, recordes |
| **Ler** | 9 artigos curtos (mente, corpo, sexualidade, apoio), cada um com as fontes no fim |

Os **Ajustes** (controle inteligente, modo foco, sons e trilha, privacidade, visual Noite/Dia, idioma,
ajuda, licença) abrem pelo emblema no alto da tela Hoje.

Também: tela de boas-vindas e apresentação guiada, "Como usar" sempre disponível, dicas na primeira
visita a cada parte ("ir descobrindo"), codinome anônimo com emblema gerado pelo app.

## Idiomas

O app segue o idioma do celular: **português**, **inglês** ou **espanhol** (outros idiomas caem no inglês).
Também dá para escolher em Ajustes → Idioma. Traduzidos: todas as telas, os 9 artigos (com as mesmas fontes;
no artigo de ajuda, os serviços do Brasil viram linhas de apoio de outros países), datas, horas e números.

- Textos das telas: `app/src/main/res/values/strings.xml` (inglês, o padrão), `values-pt/` e `values-es/`.
  As três listas precisam ter as mesmas chaves e os mesmos `%1$s`; o `LocalizationTest` confere.
- Artigos: `app/src/main/assets/artigos/pt`, `en` e `es`, com o mesmo `id`, `ordem`, `tema` e `minutos`
  em cada idioma (o `ContentTest` confere).
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

Premiam constância, não "fazer mais sessões". Teto de 40 pontos por dia.

| Ação | Pontos | Limite |
|---|---|---|
| Sessão concluída com liberação | 10 | 1 sessão pontuada por dia |
| Sessão concluída sem liberação | 15 | (idem) |
| "Não aguentei" marcado com honestidade | 3 | (idem) |
| Check-in: um dia longe da pornografia | 10 | 1×/dia |
| Respirar até o fim (2 min) | 5 | 3×/dia |
| Jogar pelo menos 3 min | 3 | 2×/dia |
| Ler um artigo até o fim | 5 | 3×/dia, cada artigo uma vez |

Sessões só pontuam com o modo foco ligado e a tela fixada. O nível conta os pontos dos últimos 28 dias:
Bronze 60, Prata 180, Ouro 360, Platina 540, Diamante 720, Invicto 900 (Invicto também pede 14 dias
seguidos de constância e 14 dias sem "não aguentei" nem sessão interrompida). Cada nível libera uma cor
de destaque enquanto a pessoa estiver nele.

## Trilha e sons

Nada de arquivos de áudio de terceiros: a trilha do Respirar (`audio/Ambient.kt`) é sintetizada ao vivo
(acordes lentos, "mar" de ruído filtrado que acompanha a respiração, sinos pentatônicos, reverberação) e
os sons dos jogos (`audio/GameSounds.kt`) são gerados na primeira abertura.

## Compilar

Precisa do JDK 17+ e do Android SDK (plataforma 35).

```
./gradlew assembleRelease      # APK assinado em app/build/outputs/apk/release/
./gradlew testDebugUnitTest    # testes da lógica, das telas e dos fluxos completos
```

A assinatura usa `keystore/compasso.jks` com as senhas de `keystore.properties`.
**Guarde esses dois arquivos fora do repositório público**: atualizações do app precisam da mesma chave;
com outra chave o Android obriga a desinstalar antes (e o histórico se perde).

## Licença

- **Código:** GNU Affero General Public License v3.0 (`LICENSE`). Copyright (C) 2026 H3yK0 (`NOTICE`).
- **Artigos** (`app/src/main/assets/artigos`, nos três idiomas): Creative Commons BY-SA 4.0.
- **Fonte Inter** (`app/src/main/res/font/inter.ttf`, recortada): SIL Open Font License 1.1
  (`app/src/main/assets/inter-ofl.txt`).
- **Fonte Cormorant Garamond** (`app/src/main/res/font/cormorant_*.ttf`): SIL Open Font License 1.1
  (`app/src/main/assets/cormorant-ofl.txt`).

Quem distribuir o APK, original ou modificado, deve oferecer junto o código-fonte correspondente, sob a mesma licença
(AGPL v3.0). Versões modificadas devem avisar que foram alteradas.
