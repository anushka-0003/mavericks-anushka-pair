package com.example.model

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable


class Wallet(var freeAmount:Int = 0, var lockedAmount:Int = 0) {
}