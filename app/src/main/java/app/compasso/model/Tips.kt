package app.compasso.model

/**
 * Dicas do modo "ir descobrindo": cada uma aparece uma vez, na primeira visita ao lugar
 * onde ela faz sentido. Dá para desligar ou rever todas nos ajustes.
 */
enum class Tip(val title: String, val message: String) {
    TRAIN(
        "Comece pelo Fácil",
        "Deslize os cartões para escolher o nível e toque em Começar. Siga a batida, pare quando a tela mandar e use “Estou perto” sempre que precisar.",
    ),
    REFUGE(
        "Seu lugar seguro",
        "Quando a vontade apertar, venha pra cá: respire 2 minutos ou jogue 3. A vontade vem em ondas e passa.",
    ),
    PROGRESS(
        "Aqui vale constância",
        "Os pontos premiam aparecer todo dia, não fazer mais sessões. Os níveis contam só os últimos 28 dias: é preciso manter.",
    ),
    READ(
        "Leituras curtas, com fontes",
        "Cada artigo cita os estudos no final. Ler até o fim rende pontos uma vez por artigo.",
    ),
    SETTINGS(
        "Decisões com calma",
        "Deixar o controle mais leve ou desligar o modo foco só vale 24 h depois. Mais firme vale na hora.",
    ),
    SUMMARY(
        "Pontos de constância",
        "Só a primeira sessão do dia vale pontos, e só com a tela fixada no modo foco. Respirar, jogar e ler também contam.",
    ),
}
