package app.compasso.data

import android.os.SystemClock
import app.compasso.model.TrustedTime

/** Horário que o app usa para regras e pontos: não volta para trás (ver [TrustedTime]). */
class AppClock(private val store: Store) {
    private var mark: TrustedTime.Mark? = store.loadClock()

    fun now(): Long {
        val next = TrustedTime.next(mark, System.currentTimeMillis(), SystemClock.elapsedRealtime())
        if (next != mark) {
            mark = next
            store.saveClock(next)
        }
        return next.trusted
    }
}
