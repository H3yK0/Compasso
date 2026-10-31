package app.compasso.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.compasso.data.Store
import app.compasso.model.Kegel

/**
 * Chega no horário de um lembrete (e também depois de reiniciar o celular, trocar a hora ou o fuso,
 * ou atualizar o app, para reagendar). Só avisa se ainda faltar Kegel no dia.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        val settings = store.loadSettings()
        val now = System.currentTimeMillis()
        if (intent.action == Reminders.ACTION_FIRE && settings.kegelReminders) {
            if (Kegel.today(store.loadKegelLog(), now) < Kegel.PER_DAY) Reminders.show(context)
        }
        Reminders.schedule(context, settings, now)
    }
}
