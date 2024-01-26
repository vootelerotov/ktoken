# What is this

Stripped down, ~un-tested~ tested fork of [Stoken](https://github.com/cernekee/stoken) in Kotlin.

To use, get token from stoken using:

`stoken --export`

and then 
```
val tokenCodeGenerator = TokenCreator().createTokenCodeGeneratorFromString(token)

val tokenCode = tokenCodeGenerator.generateTokenCode(timestamp)
```

# Why

Bored on Sunday and wanted to get a better understanding of how OTP works.

# Licence

License: LGPLv2.1+
