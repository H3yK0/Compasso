package app.compasso.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.compasso.MainActivity
import app.compasso.R
import app.compasso.data.Settings
import app.compasso.data.Store
import app.compasso.ui.AppLocale
import java.time.Instant
import java.time.ZoneId

/**
 * Lembretes do Kegel no celular: notificações locais, discretas e opcionais. Nada sai do aparelho.
 *
 * - Agenda só o próximo horário, com [AlarmManager.setWindow] (sem alarme exato: não precisa de
 *   permissão especial e respeita a economia de bateria). No horário, o [ReminderReceiver] avisa (se
 *   ainda faltar Kegel no dia) e agenda o seguinte.
 * - O texto não fala de Kegel nem de sexo, e na tela de bloqueio aparece só o nome do app.
 */
object Reminders {
    const val CHANNEL = "lembretes"
    const val NOTIFICATION_ID = 7
    const val ACTION_FIRE = "app.compasso.action.REMINDER"

    /** Extra da Activity: abrir direto numa tela. */
    const val EXTRA_OPEN = "app.compasso.extra.OPEN"
    const val OPEN_KEGEL = "kegel"

    private const val REQUEST_ALARM = 41
    private const val REQUEST_OPEN = 42

    /** Janela do alarme: chega entre o horário e 10 min depois. */
    const val WINDOW_MS = 10 * 60_000L

    /** Próximo horário depois de [now] entre os [times] (minutos desde a meia-noite), no fuso [zone]. */
    fun next(now: Long, times: List<Int>, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val valid = times.filter { it in 0 until 24 * 60 }.distinct().sorted()
        if (valid.isEmpty()) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        for (d in 0L..1L) {
            for (t in valid) {
                val at = today.plusDays(d).atTime(t / 60, t % 60).atZone(zone).toInstant().toEpochMilli()
                if (at > now) return at
            }
        }
        return null
    }

    /** Agenda o próximo lembrete (ou cancela, se estiverem desligados). */
    fun schedule(context: Context, settings: Settings, now: Long = System.currentTimeMillis()) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(context)
        am.cancel(pi)
        if (!settings.kegelReminders) return
        val at = next(now, settings.kegelTimes) ?: return
        am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pi)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Precisa pedir a permissão de notificação (Android 13+)? */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** O celular deixa o app mostrar notificações (permissão e chave do app ligadas)? */
    fun allowed(context: Context): Boolean = !needsPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Textos no idioma escolhido no app (que pode ser diferente do idioma do celular). */
    private fun localized(context: Context): Context = AppLocale.wrap(context, Store(context).loadLanguage())

    private fun ensureChannel(context: Context, text: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL, text.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = text.getString(R.string.reminder_channel_text)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    /** Mostra o lembrete. Tocar abre o app (com senha, ela é pedida antes) direto no Kegel. */
    fun show(context: Context) {
        if (!allowed(context)) return
        val text = localized(context)
        ensureChannel(context, text)
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_KEGEL)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val name = text.getString(R.string.app_name)
        // versão da tela de bloqueio: só o nome do app
        val public = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_compasso)
            .setContentTitle(name)
            .build()
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_compasso)
            .setContentTitle(name)
            .setContentText(text.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n) }
    }

    /** Some com o lembrete que estiver na tela (por exemplo, quando o Kegel do dia foi feito). */
    fun clear(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }
}
