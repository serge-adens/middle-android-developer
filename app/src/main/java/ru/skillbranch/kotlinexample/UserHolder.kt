package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email = email, password = password)
        .also { user ->
            if (map.containsKey(user.login))
                throw IllegalArgumentException("A user with this email already exists")
            else
                map[user.login] = user
        }

    fun registerUserByPhone(fullName: String, rawPhone: String): User =
        User.makeUser(fullName, phone = rawPhone).also { user ->
            if (map.containsKey(user.login))
                throw IllegalArgumentException("A user with this phone already exists")
            else
                map[user.login] = user
        }

    fun loginUser(login: String, password: String): String? {
        val normalizedPhone = login.normalizePhone()
        val user = map[login.toLowerCase()] ?: map[normalizedPhone]
        return user?.let {
            if (it.checkPassword(password)) it.userInfo
            else null
        }
    }

    fun requestAccessCode(phone: String): Unit {
        val normalizedPhone = phone.normalizePhone()
        val user = map[normalizedPhone]
        user?.let {
            it.newAccessCode(normalizedPhone)
        }
    }

    fun importUsers(list: List<String>): List<User> {
// Полное имя пользователя; email; соль:хеш пароля; телефон
// John Doe ;JohnDoe@unknow.com;[B@7591083d:c6adb4becdc64e92857e1e2a0fd6af84;;
        return list.map {
            val fields = it.split(";").map { it -> it.trim() }
            val fullname = fields[0]
            val email = fields[1].toNullIfEmpty()
            val salthash = fields[2].toNullIfEmpty()
            val phone = fields[3].toNullIfEmpty()
            User.makeUser(fullname, email, null, phone, salthash).also { user ->
                val login = user.login
                if (map.containsKey(login))
                    throw IllegalArgumentException("A user with this login already exists")
                else
                    map[login] = user
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}

fun String.normalizePhone(): String {
    return this.replace("""[^+\d]""".toRegex(), "")
}

fun String.toNullIfEmpty(): String? {
    return this.ifEmpty { null }
}
