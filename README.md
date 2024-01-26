# What is this

Stripped down fork of [Stoken](https://github.com/cernekee/stoken) in Kotlin,
implementing the very basic functionality of generating token codes from a token string.

To use, get token from `stoken` using:

`stoken --export`

and then 
```
val tokenCodeGenerator = TokenCreator().createTokenCodeGeneratorFromString(token)

val tokenCode = tokenCodeGenerator.generateTokenCode(timestamp)
```

# Licence

License: LGPLv2.1+
