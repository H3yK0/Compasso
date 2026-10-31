package app.compasso.model

import androidx.annotation.StringRes
import app.compasso.R

/**
 * Dicas do modo "ir descobrindo": cada uma aparece uma vez, na primeira visita ao lugar
 * onde ela faz sentido. Dá para desligar ou rever todas nos ajustes.
 */
enum class Tip(@StringRes val title: Int, @StringRes val message: Int) {
    TRAIN(R.string.tip_train_title, R.string.tip_train_text),
    REFUGE(R.string.tip_refuge_title, R.string.tip_refuge_text),
    KEGEL(R.string.tip_kegel_title, R.string.tip_kegel_text),
    PROGRESS(R.string.tip_progress_title, R.string.tip_progress_text),
    READ(R.string.tip_read_title, R.string.tip_read_text),
    SETTINGS(R.string.tip_settings_title, R.string.tip_settings_text),
    SUMMARY(R.string.tip_summary_title, R.string.tip_summary_text),
}
