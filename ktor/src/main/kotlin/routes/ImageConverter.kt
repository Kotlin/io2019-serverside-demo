package com.jetbrains.ktorServer.routes

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun monochrome(input: ByteArray): ByteArray {
    val stream = ByteArrayInputStream(input)
    val img = ImageIO.read(stream)
    val width = img.width
    val height = img.height
    for (h in 0 until height) {
        for (w in 0 until width) {
            var pixel = img.getRGB(w, h)
            val a = pixel shr 24 and 0xff
            val r = pixel shr 16 and 0xff
            val g = pixel shr 8 and 0xff
            val b = pixel and 0xff
            val avg = (r + g + b) / 3
            pixel = a shl 24 or (avg shl 16) or (avg shl 8) or avg
            img.setRGB(w, h, pixel)
        }
    }
    val output = ByteArrayOutputStream()
    ImageIO.write(img, "png", output)
    return output.toByteArray()
}