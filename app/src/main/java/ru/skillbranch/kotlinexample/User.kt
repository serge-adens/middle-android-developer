package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // For email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String?,
        salthash: String?
    ) : this(
        firstName,
        lastName,
        email = email,
        meta = if (!salthash.isNullOrBlank()) mapOf("src" to "csv") else mapOf("auth" to "password")
    ) {
        println("Secondary email constructor")
        when {
            !password.isNullOrBlank() -> passwordHash = encrypt(password)
            !salthash.isNullOrBlank() -> setSaltHash(salthash)
            else -> throw IllegalArgumentException("password or salthash must not be null or blank")
        }
    }

    // For phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String,
        salthash: String?
    ) : this(
        firstName,
        lastName,
        rawPhone = rawPhone,
        meta = if (!salthash.isNullOrBlank()) mapOf("src" to "csv") else mapOf("auth" to "sms")
    ) {
        println("Secondary phone constructor")
        when {
            !salthash.isNullOrBlank() -> setSaltHash(salthash)
            !phone.isNullOrBlank() -> newAccessCode(phone!!)
            else -> throw IllegalArgumentException("phone or salthash must not be null or blank")
        }

    }

    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) { "FirstName must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Email or phone must not be null or blank" }

        phone = rawPhone
        val locPhone = phone
        if (locPhone != null && !locPhone.matches("""^\+\d{11}$""".toRegex())) {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }

        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun newAccessCode(phone: String) {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Checking passwordHash is $passwordHash")
    }

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Password $oldPass has been changed on new password $newPass")
        } else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun setSaltHash(salthash: String) {
        val values = salthash.split(":")
        check(values.count() == 2)
        salt = values[0]
        passwordHash = values[1]
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt: $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null,
            salthash: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone, salthash)
                !email.isNullOrBlank() && (!password.isNullOrBlank() || !salthash.isNullOrBlank()) -> User(
                    firstName,
                    lastName,
                    email,
                    password,
                    salthash
                )
                else -> throw IllegalArgumentException("Email and (phone or salthash) must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
            this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first name and last name, current split " +
                                    "result: ${this@fullNameToPair}"
                        )
                    }
                }
    }
}