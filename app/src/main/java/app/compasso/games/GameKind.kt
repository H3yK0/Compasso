package app.compasso.games

import androidx.annotation.StringRes
import app.compasso.R

/** Os jogos do Refúgio. Originais do Compasso (nenhum usa marca ou arte de outro jogo). */
enum class GameKind(@StringRes val title: Int, @StringRes val subtitle: Int, @StringRes val howTo: Int) {
    BLOCKS(R.string.game_blocks, R.string.game_blocks_sub, R.string.game_blocks_howto),
    MERGE(R.string.game_merge, R.string.game_merge_sub, R.string.game_merge_howto),
    ECHO(R.string.game_echo, R.string.game_echo_sub, R.string.game_echo_howto),
}
