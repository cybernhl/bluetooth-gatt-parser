package org.sputnikdev.bluetooth.gattparser.spec // <-- 套件名稱必須是這個！

/**
 * 这是一个輔助類別，因為它與 Characteristic.java 在同一個 package 中，
 * 所以它可以合法地呼叫 Characteristic 的 package-private 方法。
 */
object CharacteristicValidator {
    /**
     * 強制將一個 Characteristic 物件標記為「可讀」，繞過函式庫的內部驗證。
     * @param characteristic 要修改的特徵物件
     */
    fun forceRead(characteristic: Characteristic?) {
        characteristic?.setValidForRead(true)
    }
}
