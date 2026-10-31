package app.compasso

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import app.compasso.data.Store
import app.compasso.model.Kegel
import app.compasso.model.KegelEntry
import app.compasso.notify.ReminderReceiver
import app.compasso.notify.Reminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Lembretes do Kegel no celular: horário do próximo, agenda, aviso discreto e quando não avisar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val am get() = app.getSystemService(AlarmManager::class.java)
    private val nm get() = app.getSystemService(NotificationManager::class.java)

    @Before
    fun clean() {
        app.getSharedPreferences("compasso", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        nm.cancelAll()
    }

    private fun ms(day: Int, h: Int, m: Int = 0, zone: ZoneId = ZoneOffset.UTC) =
        LocalDateTime.of(2026, 3, day, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun nextPicksTheNextTimeTodayOrTomorrow() {
        val times = listOf(9 * 60, 14 * 60, 20 * 60)
        assertEquals(ms(10, 9), Reminders.next(ms(10, 8, 30), times, ZoneOffset.UTC))
        assertEquals(ms(10, 14), Reminders.next(ms(10, 9), times, ZoneOffset.UTC)) // exatamente no horário: o próximo
        assertEquals(ms(10, 20), Reminders.next(ms(10, 15), times, ZoneOffset.UTC))
        assertEquals(ms(11, 9), Reminders.next(ms(10, 21), times, ZoneOffset.UTC))
        // fora de ordem, repetidos ou inválidos: tanto faz
        assertEquals(ms(10, 14), Reminders.next(ms(10, 10), listOf(20 * 60, 14 * 60, 14 * 60, 5000), ZoneOffset.UTC))
        assertNull(Reminders.next(ms(10, 10), emptyList(), ZoneOffset.UTC))
        // fuso com horário de verão
        val sp = ZoneId.of("America/Sao_Paulo")
        assertEquals(ms(10, 9, zone = sp), Reminders.next(ms(10, 7, zone = sp), times, sp))
    }

    @Test
    fun schedulesOnlyWhenTurnedOn() {
        val store = Store(app)
        Reminders.schedule(app, store.loadSettings())
        assertNull(shadowOf(am).nextScheduledAlarm)

        val on = store.loadSettings().copy(kegelReminders = true)
        store.saveSettings(on)
        val now = System.currentTimeMillis()
        Reminders.schedule(app, on, now)
        val alarm = shadowOf(am).nextScheduledAlarm
        assertNotNull(alarm)
        assertEquals(Reminders.next(now, on.kegelTimes), alarm!!.triggerAtTime)
        // sem alarme exato: janela de alguns minutos
        assertEquals(Reminders.WINDOW_MS, alarm.windowLengthMs)

        Reminders.schedule(app, on.copy(kegelReminders = false), now)
        assertNull(shadowOf(am).nextScheduledAlarm)
    }

    @Test
    fun firesADiscreetNotificationAndSchedulesTheNext() {
        val store = Store(app)
        store.saveSettings(store.loadSettings().copy(kegelReminders = true))
        ReminderReceiver().onReceive(app, Intent(app, ReminderReceiver::class.java).setAction(Reminders.ACTION_FIRE))

        val posted = shadowOf(nm).allNotifications
        assertEquals(1, posted.size)
        val n = posted.single()
        val text = shadowOf(n).contentText.toString()
        assertEquals(app.getString(R.string.reminder_text), text)
        // nada de "Kegel" no texto; na tela de bloqueio, só o nome do app
        assertFalse(text.contains("Kegel", ignoreCase = true))
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, n.visibility)
        assertNotNull(n.publicVersion)
        assertEquals("Compasso", shadowOf(n.publicVersion).contentTitle.toString())
        assertNull(n.publicVersion.extras.getCharSequence(android.app.Notification.EXTRA_TEXT))
        // tocar abre o app direto no Kegel
        val open = shadowOf(n.contentIntent).savedIntent
        assertEquals(Reminders.OPEN_KEGEL, open.getStringExtra(Reminders.EXTRA_OPEN))
        // o próximo já ficou agendado
        assertNotNull(shadowOf(am).nextScheduledAlarm)
    }

    @Test
    fun staysQuietWhenTodaysKegelIsDoneOrRemindersAreOff() {
        val store = Store(app)
        store.saveSettings(store.loadSettings().copy(kegelReminders = true))
        val now = System.currentTimeMillis()
        store.saveKegelLog(List(Kegel.PER_DAY) { KegelEntry(now - it * 1_000L, 3, 2, 15) })
        ReminderReceiver().onReceive(app, Intent(app, ReminderReceiver::class.java).setAction(Reminders.ACTION_FIRE))
        assertTrue(shadowOf(nm).allNotifications.isEmpty())

        store.saveKegelLog(emptyList())
        store.saveSettings(store.loadSettings().copy(kegelReminders = false))
        ReminderReceiver().onReceive(app, Intent(app, ReminderReceiver::class.java).setAction(Reminders.ACTION_FIRE))
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
        assertNull(shadowOf(am).nextScheduledAlarm)
    }

    @Test
    fun withoutPermissionNothingIsShown() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val store = Store(app)
        store.saveSettings(store.loadSettings().copy(kegelReminders = true))
        assertTrue(Reminders.needsPermission(app))
        ReminderReceiver().onReceive(app, Intent(app, ReminderReceiver::class.java).setAction(Reminders.ACTION_FIRE))
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun rebootReschedules() {
        val store = Store(app)
        store.saveSettings(store.loadSettings().copy(kegelReminders = true))
        ReminderReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(shadowOf(nm).allNotifications.isEmpty()) // reiniciar não avisa, só agenda
        assertNotNull(shadowOf(am).nextScheduledAlarm)
    }

    @Test
    fun savedTimesAreSanitized() {
        val store = Store(app)
        app.getSharedPreferences("compasso", Context.MODE_PRIVATE).edit().putString("kegelTimes", "1200,abc,540,540,99999").commit()
        assertEquals(listOf(540, 1200), store.loadSettings().kegelTimes)
        app.getSharedPreferences("compasso", Context.MODE_PRIVATE).edit().putString("kegelTimes", "").commit()
        assertEquals(listOf(540, 840, 1200), store.loadSettings().kegelTimes)
    }
}
