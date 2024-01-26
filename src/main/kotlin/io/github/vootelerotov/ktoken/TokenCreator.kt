package io.github.vootelerotov.ktoken

import kotlin.experimental.or
import kotlin.experimental.xor

val STOKEN_MAGIC = listOf("D8", "F5", "32", "53", "82", "89").map { it.toUByte(16).toByte() }.toByteArray()

const val MAX_TOKEN_BITS = 255

const val TOKEN_BITS_PER_CHAR = 3

const val VER_LENGTH = 1
const val SERIAL_LENGTH = 12

const val BIN_ENC_BITS = 189
const val BINENC_OFFSET = VER_LENGTH + SERIAL_LENGTH

const val AES_KEY_SIZE = 16

const val FLD_DIGIT_SHIFT = 6
const val FLD_DIGIT_MASK = 0x07 shl FLD_DIGIT_SHIFT

const val FLD_NUMSECONDS_SHIFT = 0
const val FLD_NUMSECONDS_MASK = 0x03 shl FLD_NUMSECONDS_SHIFT


class TokenCreator(private val encryptor: Encryptor = Encryptor()) {

  fun createTokenCodeGeneratorFromString(token: String): TokenCodeGenerator {
    val tokenAsciiBytes = token.toByteArray(Charsets.US_ASCII)

    val serial = tokenAsciiBytes.sliceArray(VER_LENGTH until  VER_LENGTH + SERIAL_LENGTH).toString(Charsets.US_ASCII)

    val numinputToBits = extractTokenBits(tokenAsciiBytes, BIN_ENC_BITS, BINENC_OFFSET)
    val encryptedSeed = numinputToBits.sliceArray(0 until AES_KEY_SIZE)
    val seedHash = getBits(numinputToBits, 159, 15 )

    val decryptSeed = decryptSeed(encryptedSeed, seedHash)

    val flags = getBits(numinputToBits, 128, 16)
    val interval = if ((flags.and(FLD_NUMSECONDS_MASK) shr FLD_NUMSECONDS_SHIFT) == 0) 30 else 60
    val digits = (flags.and(FLD_DIGIT_MASK) shr FLD_DIGIT_SHIFT) + 1

    return TokenCodeGenerator(serial, decryptSeed, digits, interval, encryptor)
  }

  private fun decryptSeed(encSeed: ByteArray, seedHash: Int): ByteArray {
    val keyHash = securidMac(STOKEN_MAGIC)
    val seed = encryptor.decrypt(keyHash, encSeed)
    val calcSeedHash = shortHash(securidMac(seed))
    if (calcSeedHash != seedHash) {
      throw IllegalStateException("Seed decryption failed")
    }
    return seed
  }

  private fun shortHash(bytes: ByteArray) =
    (bytes[0].toUByte().toInt() shl 7).or(bytes[1].toUByte().toInt() shr  1)


  private fun extractTokenBits(data: ByteArray, numberOfBits: Int, offset: Int = 0): ByteArray {
    var bitpos = 13
    val out = ByteArray(MAX_TOKEN_BITS / 8 + 2)
    var pos = 0
    for (t in data.sliceArray(offset until offset + numberOfBits / TOKEN_BITS_PER_CHAR)) {
      var decoded = (t - '0'.code) and 0x07
      decoded = decoded shl bitpos
      out[0 + pos] = out[0 + pos] or (decoded shr 8).toByte()
      out[1 + pos] = out[1 + pos] or (decoded and 0xFF).toByte()
      bitpos = bitpos - TOKEN_BITS_PER_CHAR
      if (bitpos < 0) {
        bitpos = bitpos + 8
        pos = pos + 1
      }
    }
    return out
  }

  private fun getBits(data: ByteArray, bitOffset: Int, length: Int): Int {
    val bits = bitOffset.until(bitOffset + length).map { index ->
      if ((data[index / 8].toInt().shl(index % 8) and 0b10000000) != 0) 1 else 0
    }
    return bits.fold(0) { acc, bit -> acc.shl(1).or(bit) }
  }

  private fun securidMac(data: ByteArray): ByteArray {
    val pad = ByteArray(AES_KEY_SIZE)
    var p = AES_KEY_SIZE - 1
    var i = data.size * 8
    while (i > 0) {
      pad[p] = (i % 256).toByte()
      p = p - 1
      i = i shr 8
    }
    var odd = false
    var t = data
    var work = ByteArray(AES_KEY_SIZE) { 0xFF.toByte() }
    while (t.size > AES_KEY_SIZE) {
      work = encryptAndXor(t.sliceArray(0 until AES_KEY_SIZE), work)
      t = t.sliceArray(AES_KEY_SIZE until t.size)
      odd = !odd
    }
    work = encryptAndXor(t + ByteArray(AES_KEY_SIZE - t.size), work)
    if (odd) {
      val zero = ByteArray(AES_KEY_SIZE)
      work = encryptAndXor(zero, work)
    }
    work = encryptAndXor(pad, work)
    return encryptAndXor(work, work)
  }

  private fun encryptAndXor(key: ByteArray, data: ByteArray) =
    encryptor.encrypt(key, data).zip(data).map { (encrypted, original) -> encrypted.xor(original) }.toByteArray()

}
