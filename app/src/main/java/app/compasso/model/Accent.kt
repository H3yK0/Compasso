package app.compasso.model

import androidx.annotation.StringRes
import app.compasso.R

/** Cores de destaque do app. As especiais são regalias liberadas ao alcançar cada nível. */
enum class Accent(@StringRes val label: Int, val unlock: Tier) {
    TEAL(R.string.accent_teal, Tier.START),
    AMBER(R.string.accent_amber, Tier.BRONZE),
    OCEAN(R.string.accent_ocean, Tier.SILVER),
    GOLD(R.string.accent_gold, Tier.GOLD),
    LAVENDER(R.string.accent_lavender, Tier.PLATINUM),
    EMERALD(R.string.accent_emerald, Tier.DIAMOND),
    CRIMSON(R.string.accent_crimson, Tier.UNBEATEN),
}
