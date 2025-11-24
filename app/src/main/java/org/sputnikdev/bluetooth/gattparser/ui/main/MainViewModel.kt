package org.sputnikdev.bluetooth.gattparser.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory
import org.sputnikdev.bluetooth.gattparser.GattResponse
import org.sputnikdev.bluetooth.gattparser.spec.BluetoothGattSpecificationReader
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic
import org.sputnikdev.bluetooth.gattparser.spec.CharacteristicValidator // <-- 匯入我們新的輔助類別

class MainViewModel : ViewModel() {

    private val _validationResults = MutableLiveData<String>()
    val validationResults: LiveData<String> = _validationResults

    private val specReader: BluetoothGattSpecificationReader =
        BluetoothGattParserFactory.getSpecificationReader()

    fun runGattParserValidation() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = StringBuilder("GATT Parser Validation Results (Expanded):\n\n")

            try {
                // 依序執行所有測試案例
                results.append(runHeartRateTest())
                results.append(runBloodPressureTest())
                results.append(runCyclingPowerTest())
                results.append(runCurrentTimeTest()) // <-- 新增
                results.append(runFirmwareRevisionTest()) // <-- 新增
                results.append(runBatteryLevelTest()) // <-- 新增
                results.append(runMifloraSensorTest()) // <-- 新增

            } catch (e: Exception) {
                results.append("\n!!! AN UNEXPECTED ERROR OCCURRED !!!\n")
                results.append("Error Type: ${e.javaClass.simpleName}\n")
                results.append("Message: ${e.message}\n")
                results.append("Stack Trace:\n${e.stackTraceToString().take(1000)}...")
            }

            _validationResults.postValue(results.toString())
        }
    }

    private fun runHeartRateTest(): String {
        val testLog = StringBuilder("--- Test 1: Heart Rate ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        // Flag 0x14 -> uint16, RR-Interval present
        val rawData = byteArrayOf(0x14, 74, 13, 3)

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.heart_rate_measurement")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }

        val uuid = characteristic.uuid
        val result: GattResponse = parser.parse(uuid, rawData)

        // 使用與 GenericCharacteristicParserIntegrationTest.java 一致的精確欄位名稱
        val heartRate = result.get("Heart Rate Measurement Value (uint16)")?.integer
        val rrInterval = result.get("RR-Interval")?.integer

        testLog.append("[OK] Parsed using UUID: $uuid.\n")
        testLog.append(" -> Heart Rate: $heartRate bpm\n")
        testLog.append(" -> RR-Interval: $rrInterval\n")
        testLog.append("[SUCCESS] Heart Rate test completed.\n\n")
        return testLog.toString()
    }

    private fun runBloodPressureTest(): String {
        val testLog = StringBuilder("--- Test 2: Blood Pressure ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        val rawData = byteArrayOf(0x01, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00) // 修正了範例資料長度

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.blood_pressure_measurement")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }
        val uuid = characteristic.uuid
        val result: GattResponse = parser.parse(uuid, rawData)

        val systolic = result.get("Systolic")?.float
        val diastolic = result.get("Diastolic")?.float
        val meanArterialPressure = result.get("Mean Arterial Pressure")?.float

        testLog.append("[OK] Parsed using UUID: $uuid.\n")
        testLog.append(" -> Systolic: $systolic kPa\n")
        testLog.append(" -> Diastolic: $diastolic kPa\n")
        testLog.append(" -> Mean Arterial Pressure: $meanArterialPressure kPa\n")
        testLog.append("[SUCCESS] Blood Pressure test completed.\n\n")
        return testLog.toString()
    }

    private fun runCyclingPowerTest(): String {
        val testLog = StringBuilder("--- Test 3: Cycling Power ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        val rawData = byteArrayOf(0x3A, 0x00, 100, 0x00, 60)

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.cycling_power_measurement")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }

        // 【關鍵步驟】使用我們的輔助類別，強制將這個特徵設為可讀
        CharacteristicValidator.forceRead(characteristic)

        val uuid = characteristic.uuid
        val result: GattResponse = parser.parse(uuid, rawData)

        val power = result.get("Instantaneous Power")?.integer
        val pedalBalance = result.get("Pedal Power Balance")?.integer

        testLog.append("[OK] Parsed using helper to bypass validation.\n")
        testLog.append(" -> Instantaneous Power: $power Watts\n")
        testLog.append(" -> Pedal Power Balance: $pedalBalance %\n")
        testLog.append("[SUCCESS] Cycling Power test completed.\n\n")
        return testLog.toString()
    }

    /**
     * 新增：來自 GenericCharacteristicParserIntegrationTest.java
     * 驗證一個包含多個欄位的固定結構特徵。
     */
    private fun runCurrentTimeTest(): String {
        val testLog = StringBuilder("--- Test 4: Current Time ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        // 2017-01-04 11:38:45, Day of Week: 3, Adjust Reason: Manual time update
        val rawData = byteArrayOf(0xE1.toByte(), 0x07, 1, 4, 11, 38, 45, 3, 1)

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.current_time")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }

        val uuid = characteristic.uuid
        val result = parser.parse(uuid, rawData)

        val year = result.get("Year")?.integer
        val month = result.get("Month")?.integer
        val day = result.get("Day")?.integer
        val hours = result.get("Hours")?.integer
        val minutes = result.get("Minutes")?.integer
        val seconds = result.get("Seconds")?.integer
        val dayOfWeek = result.get("Day of Week")?.string
        val adjustReason = result.get("Adjust Reason")?.string

        testLog.append("[OK] Parsed 'current_time' using UUID: $uuid.\n")
        testLog.append(" -> Datetime: $year-$month-$day $hours:$minutes:$seconds\n")
        testLog.append(" -> Day of Week: $dayOfWeek\n")
        testLog.append(" -> Adjust Reason: $adjustReason\n")
        testLog.append("[SUCCESS] Current Time test completed.\n\n")
        return testLog.toString()
    }

    /**
     * 新增：來自 GenericCharacteristicParserIntegrationTest.java
     * 驗證一個簡單的 UTF-8 字串特徵。
     */
    private fun runFirmwareRevisionTest(): String {
        val testLog = StringBuilder("--- Test 5: Firmware Revision ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        val rawData = "2.1".toByteArray(Charsets.UTF_8) // "2.1" -> [50, 46, 49]

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.firmware_revision_string")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }

        val uuid = characteristic.uuid
        val result = parser.parse(uuid, rawData)

        val firmware = result.get("Firmware Revision")?.string

        testLog.append("[OK] Parsed 'firmware_revision_string' using UUID: $uuid.\n")
        testLog.append(" -> Firmware: $firmware\n")
        testLog.append("[SUCCESS] Firmware Revision test completed.\n\n")
        return testLog.toString()
    }

    /**
     * 新增：來自 GenericCharacteristicParserIntegrationTest.java
     * 驗證最常見的「電池電量」特徵。
     */
    private fun runBatteryLevelTest(): String {
        val testLog = StringBuilder("--- Test 6: Battery Level ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        val rawData = byteArrayOf(51) // 電量 51%

        val characteristic: Characteristic? = specReader.getCharacteristicByType("org.bluetooth.characteristic.battery_level")
        if (characteristic == null) {
            return testLog.append("[FAILURE] Could not find spec.\n\n").toString()
        }

        val uuid = characteristic.uuid
        val result = parser.parse(uuid, rawData)

        val level = result.get("Level")?.integer

        testLog.append("[OK] Parsed 'battery_level' using UUID: $uuid.\n")
        testLog.append(" -> Level: $level %\n")
        testLog.append("[SUCCESS] Battery Level test completed.\n\n")
        return testLog.toString()
    }

    /**
     * 新增：來自 GenericCharacteristicParserIntegrationTest.java (testMiflora)
     * 驗證一個協力廠商（小米花花草草監測儀）的自訂特徵。
     */
    private fun runMifloraSensorTest(): String {
        val testLog = StringBuilder("--- Test 7: Miflora Sensor Data ---\n")
        val parser: BluetoothGattParser = BluetoothGattParserFactory.getDefault()
        // 溫度、光照、濕度、肥力等資料
        val rawData = byteArrayOf(0x1f, 0x01, 0x00, 0xe7.toByte(), 0x26, 0x00, 0x00, 0x35, 0x74, 0x00, 0x02, 0x3c, 0x00, 0xfb.toByte(), 0x34, 0x9b.toByte())

        // 注意：這是一個自訂的 UUID
        val uuid = "00001a01-0000-1000-8000-00805f9b34fb"
        val result = parser.parse(uuid, rawData)

        val temperature = result.get("Temperature")?.double
        val sunlight = result.get("Sunlight")?.integer
        val moisture = result.get("Moisture")?.integer
        val fertility = result.get("Fertility")?.integer

        testLog.append("[OK] Parsed Miflora custom characteristic: $uuid.\n")
        testLog.append(String.format(" -> Temperature: %.1f °C\n", temperature))
        testLog.append(" -> Sunlight: $sunlight lux\n")
        testLog.append(" -> Moisture: $moisture %\n")
        testLog.append(" -> Fertility: $fertility µS/cm\n")
        testLog.append("[SUCCESS] Miflora Sensor test completed.\n\n")
        return testLog.toString()
    }
}
