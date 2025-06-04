import android.content.Context
import android.util.Log
import com.example.obd_servise.ui.car.Car
import com.example.obd_servise.ui.car.CarPart
import com.example.obd_servise.ui.car.CarViewModel
import com.example.obd_servise.ui.settings.SettingsViewModel
import com.google.firebase.database.FirebaseDatabase

class NotificationManager(
    private val context: Context,
    private val carViewModel: CarViewModel,
    private val settingsViewModel: SettingsViewModel
) {
    private val notificationHelper = NotificationHelper(context)
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    fun checkAndSendNotifications(parts: List<CarPart>, carId: String, carName: String) {
        Log.d("NotificationManager", "checkAndSendNotifications called for car: $carName")
        if (!settingsViewModel.getNotificationsEnabled()) {
            Log.d("NotificationManager", "Notifications are disabled in settings")
            return
        }

        val selectedCarOnly = settingsViewModel.getSelectedCarOnly()

        // Если включено "только для выбранного авто", проверяем что это выбранный авто
        if (selectedCarOnly && !carViewModel.isCarSelected(carId)) return

        parts.filter { part ->
            part.notificationsEnabled &&
                    (part.condition == "warning" || part.condition == "critical")
        }.forEach { part ->
            sendNotificationForPart(part, carId, carName)
        }
    }

    private fun sendNotificationForPart(part: CarPart, carId: String, carName: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val notificationRef =
            firebaseDatabase.getReference("cars/$carId/notifications/$notificationId")

        val notificationText = when (part.condition) {
            "warning" -> {
                val remainingMileage = part.recommendedMileage - part.currentMileage
                "${part.name} скоро нужно будет заменить, оставшийся пробег: $remainingMileage км"
            }

            "critical" -> {
                val exceededMileage = part.currentMileage - part.recommendedMileage
                "${part.name} необходимо заменить, рекомендуемый пробег замены превышен на $exceededMileage км"
            }

            else -> return
        }

        val notification = mapOf(
            "id" to notificationId,
            "carId" to carId,
            "carName" to carName,
            "partName" to part.name,
            "status" to part.condition,
            "message" to notificationText,
            "timestamp" to System.currentTimeMillis()
        )

        // Сохраняем в Firebase с обработчиком
        notificationRef.setValue(notification).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("NotificationManager", "Notification saved to Firebase")

                // Отправляем локальное уведомление
                if (settingsViewModel.getNotificationMethods().contains("phone")) {
                    notificationHelper.showNotification(
                        title = "Уведомление для $carName",
                        message = notificationText,
                        notificationId = notificationId
                    )
                }
            } else {
                Log.e("NotificationManager", "Failed to save notification", task.exception)
            }
        }
    }

    private fun sendEmailNotification(email: String, carName: String, message: String) {
        // Реализация отправки email (используйте ваш email сервис)
    }
}