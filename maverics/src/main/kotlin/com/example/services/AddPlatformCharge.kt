package com.example.services

import com.example.constants.totalPlatformFees
import java.math.BigInteger

fun addPlatformCharge(platformCharge: Long){
    totalPlatformFees += BigInteger.valueOf(platformCharge)
}