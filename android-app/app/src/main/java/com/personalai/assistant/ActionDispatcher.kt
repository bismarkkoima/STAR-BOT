package com.personalai.assistant

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract

/**
 * Takes a tool name + args (as decided by the LLM) and actually executes it —
 * either via a native Android Intent, or by firing a named Tasker task.
 */
class ActionDispatcher(private val context: Context) {

    fun dispatch(tool: String, args: Map<String, String>) {
        when (tool) {
            "set_reminder" -> setReminder(args)
            "add_calendar_event" -> addCalendarEvent(args)
            "open_app" -> openApp(args)

            // Anything without a clean native API goes through Tasker.
            "create_note" -> fireTaskerTask("AI_CreateNote", args)
            "search_in_app" -> fireTaskerTask("AI_SearchInApp", args)
            "send_message" -> fireTaskerTask("AI_SendMessage", args)
            "read_notifications" -> fireTaskerTask("AI_ReadNotifications", args)

            "answer_only" -> { /* nothing to execute, just a reply */ }
            else -> { /* unknown tool — log/ignore */ }
        }
    }

    private fun setReminder(args: Map<String, String>) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, args["message"] ?: "Reminder")
            // TODO: parse args["time"] into hour/minute; for full datetime reminders,
            // consider AlarmManager + a broadcast receiver instead of ACTION_SET_ALARM,
            // which is designed for clock-style alarms rather than one-off future dates.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun addCalendarEvent(args: Map<String, String>) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, args["title"])
            putExtra(CalendarContract.Events.EVENT_LOCATION, args["location"])
            // TODO: convert ISO 8601 start/end from args into millis via
            // CalendarContract.EXTRA_EVENT_BEGIN_TIME / EXTRA_EVENT_END_TIME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun openApp(args: Map<String, String>) {
        val appName = args["app_name"] ?: return
        val pm = context.packageManager
        val pkg = pm.getInstalledApplications(0).firstOrNull {
            pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true)
        }?.packageName ?: return
        pm.getLaunchIntentForPackage(pkg)?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(it)
        }
    }

    /**
     * Fires a Tasker task by name via Tasker's public broadcast intent.
     * Requires: Tasker installed, and Tasker > Preferences > Misc >
     * "Allow External Access" enabled.
     */
    private fun fireTaskerTask(taskName: String, args: Map<String, String>) {
        val intent = Intent("net.dinglisch.android.tasker.ACTION_TASK").apply {
            putExtra("task_name", taskName)
            // Tasker tasks read these back via %par1, %par2... or via the
            // "Plugin" -> AutoTools JSON var approach. Simplest: pass a single
            // JSON string and %par1 in Tasker, then use AutoTools "Json Read"
            // action to pull fields out.
            putExtra("par1", org.json.JSONObject(args).toString())
        }
        context.sendBroadcast(intent)
    }
}
