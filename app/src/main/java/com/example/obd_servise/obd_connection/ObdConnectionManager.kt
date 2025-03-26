//import com.example.obd_servise.obd_connection.api.command.engine.SpeedCommand
//import com.example.obd_servise.obd_connection.api.connection.ObdDeviceConnection
//import com.example.obd_servise.obd_connection.api.command.ObdCommand
////import com.github.eltonvs.obd.ObdRawResponse
//import java.io.InputStream
//import java.io.OutputStream
//
//class ObdConnectionManager(private val inputStream: InputStream, private val outputStream: OutputStream) {
//    private val obdConnection = ObdDeviceConnection(inputStream, outputStream)
//
//    suspend fun getVehicleSpeed(): String {
//        val response = obdConnection.run(SpeedCommand())
//        return response.value
//    }
//
//    // Можно добавить другие команды аналогично
//}
