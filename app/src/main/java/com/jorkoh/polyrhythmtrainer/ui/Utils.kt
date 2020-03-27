package com.jorkoh.polyrhythmtrainer.ui

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.mutate(actions: MutableLiveData<T>.() -> Unit) {
    actions(this)
    this.value = this.value
}