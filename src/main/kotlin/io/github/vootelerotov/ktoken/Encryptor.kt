package io.github.vootelerotov.ktoken

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Encryptor(
  private val cipherSupplier: () -> Cipher = { Cipher.getInstance("AES/ECB/NoPadding") },
  private val secretKeyAlgorithm: String = "AES"
) {

  fun encrypt(key: ByteArray, data: ByteArray): ByteArray =
    cipherSupplier().also { cipher ->
      cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, secretKeyAlgorithm))
    }.update(data)

  fun decrypt(key: ByteArray, data: ByteArray): ByteArray =
    cipherSupplier().also { cipher ->
      cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, secretKeyAlgorithm))
    }.update(data)

}
