package com.example.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.os.Handler
import android.os.Looper


class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var dateTextView: TextView
    private lateinit var titleInput: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var setDateReminderBtn: Button
    private lateinit var setTimeReminderBtn: Button

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Must call before findViewById
        val aboutBtn = findViewById<Button>(R.id.aboutBtn)

        aboutBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_about_us, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView)
            val dialog = builder.create()
            dialog.show()
        }

        // This runs 1.5 seconds after launch
        Handler(Looper.getMainLooper()).postDelayed({
            val dialogView = layoutInflater.inflate(R.layout.dialog_about_us, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(dialogView)

            val dialog = builder.create()

            // Optional: handle the Close button inside the dialog XML
            val closeBtn = dialogView.findViewById<Button>(R.id.closeBtn)
            closeBtn?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }, 1500)

        calendarView = findViewById(R.id.calendarView)
        dateTextView = findViewById(R.id.idTVDate)
        titleInput = findViewById(R.id.editTextTitle)
        timePicker = findViewById(R.id.timePicker)
        setDateReminderBtn = findViewById(R.id.setDateReminderBtn)
        setTimeReminderBtn = findViewById(R.id.setTimeReminderBtn)

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "notifyChannel",
                "Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            dateTextView.text = "Selected Date: $selectedDate"

            // Show buttons after date selection if hidden
            setDateReminderBtn.visibility = View.VISIBLE
            setTimeReminderBtn.visibility = View.VISIBLE
        }

        setDateReminderBtn.setOnClickListener {
            val title = titleInput.text.toString().ifEmpty { "Reminder" }
            val calendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            setReminder(calendar.timeInMillis, title)
        }

        setTimeReminderBtn.setOnClickListener {
            val title = titleInput.text.toString().ifEmpty { "Reminder" }
            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }
            setReminder(cal.timeInMillis, title)
        }

        // Initially hide the reminder buttons until a date is selected
        setDateReminderBtn.visibility = View.GONE
        setTimeReminderBtn.visibility = View.GONE
    }

    private fun setReminder(timeInMillis: Long, message: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("TITLE", message)
        }

        // Combine FLAG_IMMUTABLE with FLAG_UPDATE_CURRENT (best practice)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // For Android 12+ (API 31+), check permission before scheduling exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                Toast.makeText(this, "Enable 'Alarms & Reminders' permission", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }

        Toast.makeText(this, "Reminder set for $message", Toast.LENGTH_SHORT).show()
    }

}