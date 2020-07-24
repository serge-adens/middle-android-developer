package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    for (index in lastIndex downTo 0) {
        if (predicate(this[index])) {
            return take(index)
        }
    }
    return emptyList()
}