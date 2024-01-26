package io.github.vootelerotov.ktoken

import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Generate RSA SecureID 128-bit (AES) tokens, like Stoken.
 *
 * Note: takes in decrypted seed as HEX string. To get it, inport token to Stoken, and use "stoken show --seed"
 */
class TokenGenerator(rawSerialNumber: String, rawDecryptedSeed: String) {

  private val datetimeToDecimalRepresentation: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMddHHmm0000")
  private val aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

  private val serialNumberSuffix: ByteArray = bytesFromHex(rawSerialNumber).copyOfRange(2, 6)
  private val decryptedSeed: ByteArray = rawDecryptedSeed
    .replace(" ", "") // stoken prints decrypted seed with spaces
    .let { bytesFromHex(it) }


  fun generateToken(timestamp: LocalDateTime): String {
    // One calculation calculates 4 codes. If the code updates every minute, then the calculations for 16:00, 16:01, 16:02 and 16:03 are the same.
    // If every 30 seconds, it should be two
    val minutesNotUsedForCalculatingKey = timestamp.minute % 4
    val adjustedTimestamp = timestamp.minusMinutes(minutesNotUsedForCalculatingKey.toLong())
    val timestampBytes = bytesFromHex(datetimeToDecimalRepresentation.format(adjustedTimestamp))

    val key = generateKey(timestampBytes)

    val token = extractTokenFromKey(key, minutesNotUsedForCalculatingKey)

    return token.toString().takeLast(8).let { it.padStart(8 - it.length, '0') }
  }

  private fun extractTokenFromKey(key: ByteArray, numberOfCodeToExtract: Int): UInt =
    ByteBuffer.wrap(key.copyOfRange(numberOfCodeToExtract * 4, numberOfCodeToExtract * 4 + 4)).getInt().toUInt()

  private fun generateKey(timestamp: ByteArray): ByteArray {
    val timestampBytesToUsePerIteration = intArrayOf(2, 3, 4, 5, 8) // magic array from Stoken
      .map { timestamp.copyOfRange(0, it) }

    return timestampBytesToUsePerIteration.fold(decryptedSeed) { key, bytesToUse ->
      encrpyt(key, calculateKeyFromTimestamp(bytesToUse))
    }
  }

  private fun calculateKeyFromTimestamp(timestampBytes: ByteArray): ByteArray {
    val paddedTimestampBytes = (timestampBytes.asSequence() + generateSequence { 0xAA.toByte() }).take(8).toList()
    val paddedSerialBytes = (serialNumberSuffix.asSequence() + generateSequence { 0xBB.toByte() }).take(8).toList()
    return (paddedTimestampBytes + paddedSerialBytes).toByteArray();
  }

  private fun bytesFromHex(hex: String): ByteArray = hex.chunked(2).map { it.toUByte(16).toByte()}.toByteArray()

  fun encrpyt(key: ByteArray, data: ByteArray): ByteArray {
    val cipher = aesCipher
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.update(data)
  }

}
