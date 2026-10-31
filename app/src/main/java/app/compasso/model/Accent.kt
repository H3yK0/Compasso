package app.compasso.model

/** Cores de destaque do app. As especiais são regalias liberadas ao alcançar cada nível. */
enum class Accent(val label: String, val unlock: Tier) {
    TEAL("Compasso", Tier.START),
    AMBER("Âmbar", Tier.BRONZE),
    OCEAN("Oceano", Tier.SILVER),
    GOLD("Ouro", Tier.GOLD),
    LAVENDER("Lavanda", Tier.PLATINUM),
    EMERALD("Esmeralda", Tier.DIAMOND),
    CRIMSON("Invicto", Tier.UNBEATEN),
}
