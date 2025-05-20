package com.mashup.com.mashup.dhc.utils

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@JvmInline
value class Money(
    @Serializable(with = BigDecimalSerializer::class) private val value: BigDecimal
) {
    constructor(value: String) : this(BigDecimal(value))

    constructor(value: Int) : this(BigDecimal(value))

    operator fun plus(other: Money): Money = Money(this.value + other.value)

    operator fun minus(other: Money): Money = Money(this.value - other.value)

    operator fun times(other: BigDecimal): Money = Money(this.value * other)

    operator fun div(other: BigDecimal): Money = Money(this.value / other)

    override fun toString(): String = value.toString()
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}