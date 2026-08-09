package com.quickbill.customerdisplay

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeBitmap {

    fun generate(
        payload: String,
        size: Int = 600
    ): Bitmap? {

        if (payload.isBlank() || size <= 0) {
            return null
        }

        return try {

            val hints = EnumMap<EncodeHintType, Any>(
                EncodeHintType::class.java
            )

            hints[EncodeHintType.MARGIN] = 1
            hints[EncodeHintType.ERROR_CORRECTION] =
                ErrorCorrectionLevel.M

            val matrix: BitMatrix =
                MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    hints
                )

            val bitmap = Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

            for (x in 0 until size) {
                for (y in 0 until size) {

                    bitmap.setPixel(
                        x,
                        y,
                        if (matrix[x, y]) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                    )
                }
            }

            bitmap

        } catch (_: Exception) {
            null
        }
    }
}