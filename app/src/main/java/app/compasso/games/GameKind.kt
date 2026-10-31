package app.compasso.games

/** Os jogos do Refúgio. Originais do Compasso (nenhum usa marca ou arte de outro jogo). */
enum class GameKind(val title: String, val subtitle: String, val howTo: String) {
    BLOCKS(
        "Encaixe",
        "Arraste as peças e complete linhas",
        "Arraste cada peça para o tabuleiro. Linha ou coluna completa some e vale pontos. O jogo acaba quando nenhuma peça cabe mais.",
    ),
    MERGE(
        "Dobra",
        "Deslize e junte números iguais",
        "Deslize para qualquer lado. Peças iguais que se encontram viram uma só, com o dobro do valor. Tente chegar a 2048.",
    ),
    ECHO(
        "Eco",
        "Repita a sequência de luzes",
        "Observe a sequência de luzes e sons e repita na mesma ordem. A cada rodada ela ganha um passo e fica um pouco mais rápida.",
    ),
}
