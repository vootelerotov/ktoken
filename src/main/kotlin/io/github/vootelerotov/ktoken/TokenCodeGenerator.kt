package io.github.vootelerotov.ktoken

import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val datetimeToDecimalRepresentation: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMddHHmm0000")


/**
 * Generate RSA SecureID 128-bit (AES) tokens, like Stoken.
 */
class TokenCodeGenerator(
  rawSerialNumber: String,
  private val decryptedSeed: ByteArray,
  private val digits: Int,
  intervalInSeconds: Int,
  private val encryptor: Encryptor,
) {

  private val interval = Duration.ofSeconds(intervalInSeconds.toLong())
  private val serialNumberSuffix: ByteArray = bytesFromHex(rawSerialNumber).copyOfRange(2, 6)

  fun generateTokenCode(timestamp: Instant): String {
    // One calculation calculates 4 codes. If the code updates every minute, then the calculations for 16:00, 16:01, 16:02 and 16:03 are the same.
    // If every 30 seconds, then 16:00 and 16:01 are the same, and 16:02 and 16:03 are the same.
    val secondsThatShareTheSameKey = interval.multipliedBy(4).toSeconds().toInt()

    val secondsNotUsedForCalculatingKey = (timestamp.epochSecond % secondsThatShareTheSameKey).toInt()
    val adjustedTimestamp = timestamp.minusSeconds(secondsNotUsedForCalculatingKey.toLong())
    val timestampBytes = bytesFromHex(datetimeToDecimalRepresentation.format(adjustedTimestamp.atZone(ZoneOffset.UTC)))

    val key = generateKey(timestampBytes)

    val token = extractTokenFromKey(key, secondsNotUsedForCalculatingKey / interval.seconds.toInt())

    return token.toString().takeLast(digits).let { it.padStart(digits - it.length, '0') }
  }

  private fun extractTokenFromKey(key: ByteArray, numberOfCodeToExtract: Int): UInt =
    ByteBuffer.wrap(key.copyOfRange(numberOfCodeToExtract * 4, numberOfCodeToExtract * 4 + 4)).getInt().toUInt()

  private fun generateKey(timestamp: ByteArray): ByteArray {
    val timestampBytesToUsePerIteration = intArrayOf(2, 3, 4, 5, 8) // magic array from Stoken
      .map { timestamp.copyOfRange(0, it) }

    return timestampBytesToUsePerIteration.fold(decryptedSeed) { key, bytesToUse ->
      encryptor.encrypt(key, calculateKeyFromTimestamp(bytesToUse))
    }
  }

  private fun calculateKeyFromTimestamp(timestampBytes: ByteArray): ByteArray {
    val paddedTimestampBytes = (timestampBytes.asSequence() + generateSequence { 0xAA.toByte() }).take(8).toList()
    val paddedSerialBytes = (serialNumberSuffix.asSequence() + generateSequence { 0xBB.toByte() }).take(8).toList()
    return (paddedTimestampBytes + paddedSerialBytes).toByteArray();
  }

  private fun bytesFromHex(hex: String): ByteArray = hex.chunked(2).map { it.toUByte(16).toByte()}.toByteArray()

}
