package zone.ien.shampoo.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.content.res.Resources
import android.util.TypedValue
import java.nio.ByteBuffer


object MyUtils {

    fun hasProperty(characteristic: BluetoothGattCharacteristic, property: Int): Boolean = (characteristic.properties and property) == property

    fun getBluetoothGattProperty(id: Int): String {
        val result = arrayListOf<String>()
        val properties = arrayListOf(
            BluetoothGattCharacteristic.PROPERTY_BROADCAST,
            BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        )

        for (property in properties) {
            if (id and property == property) {
                result.add(when (id and property) {
                    BluetoothGattCharacteristic.PROPERTY_BROADCAST -> "PROPERTY_BROADCAST"
                    BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS -> "PROPERTY_EXTENDED_PROPS"
                    BluetoothGattCharacteristic.PROPERTY_INDICATE -> "PROPERTY_INDICATE"
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY -> "PROPERTY_NOTIFY"
                    BluetoothGattCharacteristic.PROPERTY_READ -> "PROPERTY_READ"
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE -> "PROPERTY_SIGNED_WRITE"
                    BluetoothGattCharacteristic.PROPERTY_WRITE -> "PROPERTY_WRITE"
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE -> "PROPERTY_WRITE_NO_RESPONSE"
                    else -> "UNKNOWN($id)"
                })
            }
        }

        return result.joinToString(",")
    }

    fun ByteArray.toInt():Int {
        val copy = copyOf()
        if (copy.size != 4) {
            throw Exception("wrong len")
        }
        copy.reverse()
        return ByteBuffer.wrap(copy).int
    }

    fun ByteArray.toDouble(): Double {
        val copy = copyOf()
        if (copy.size != 8) {
            throw Exception("wrong len: ${copy.joinToString { "${it.toInt()}," }}")
        }
        copy.reverse()
        return ByteBuffer.wrap(copy).double
    }

    fun getAttrColor(theme: Resources.Theme, id: Int): Int = TypedValue().apply { theme.resolveAttribute(id, this, true) }.data
}