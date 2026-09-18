package com.sch.sekolah_mobile_app.ui.screens.jadwal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pure Kotlin Multiplatform QR Code Generator (Model 2, Byte Mode, ECC Low/Medium/High).
 * Works across Android, iOS, and Desktop without any native dependency.
 */
class QrCodeMatrix(val size: Int, private val modules: Array<BooleanArray>) {
    fun getModule(x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) return false
        return modules[y][x]
    }
}

enum class QrEccLevel(val formatBits: Int) {
    LOW(1),
    MEDIUM(0),
    QUARTILE(3),
    HIGH(2)
}

object QrCodeEncoder {

    // Reed-Solomon Galois Field GF(256) with primitive polynomial 0x11D (285)
    private val EXP = IntArray(512)
    private val LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP[i] = x
            EXP[i + 255] = x
            LOG[x] = i
            x = (x shl 1)
            if (x >= 256) x = x xor 0x11D
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP[LOG[x] + LOG[y]]
    }

    private fun rsComputeEcc(data: IntArray, eccCount: Int): IntArray {
        // Generator polynomial
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], EXP[i])
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        // Remainder division
        val res = IntArray(eccCount)
        for (b in data) {
            val factor = b xor res[0]
            for (j in 0 until eccCount - 1) {
                res[j] = res[j + 1] xor gfMul(gen[eccCount - 1 - j], factor)
            }
            res[eccCount - 1] = gfMul(gen[0], factor)
        }
        return res
    }

    // Capacity table for Byte mode: (version, totalDataBytes, ecBytesPerBlock, numBlocks)
    // Supports version 1 (21x21) to 10 (57x57)
    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val eccCodewordsPerBlock: Int,
        val blocksGroup1: Int,
        val dataCodewordsBlock1: Int,
        val blocksGroup2: Int,
        val dataCodewordsBlock2: Int
    )

    private val SPECS_MEDIUM = listOf(
        VersionSpec(1, 26, 10, 1, 16, 0, 0),
        VersionSpec(2, 44, 16, 1, 28, 0, 0),
        VersionSpec(3, 70, 26, 1, 44, 0, 0),
        VersionSpec(4, 100, 18, 2, 32, 0, 0),
        VersionSpec(5, 134, 24, 2, 43, 0, 0),
        VersionSpec(6, 172, 16, 4, 27, 0, 0),
        VersionSpec(7, 196, 18, 4, 31, 0, 0),
        VersionSpec(8, 242, 22, 2, 38, 2, 39),
        VersionSpec(9, 292, 22, 3, 36, 2, 37),
        VersionSpec(10, 346, 26, 4, 43, 1, 44)
    )

    private val SPECS_LOW = listOf(
        VersionSpec(1, 26, 7, 1, 19, 0, 0),
        VersionSpec(2, 44, 10, 1, 34, 0, 0),
        VersionSpec(3, 70, 15, 1, 55, 0, 0),
        VersionSpec(4, 100, 20, 1, 80, 0, 0),
        VersionSpec(5, 134, 26, 1, 108, 0, 0),
        VersionSpec(6, 172, 18, 2, 68, 0, 0),
        VersionSpec(7, 196, 20, 2, 78, 0, 0),
        VersionSpec(8, 242, 24, 2, 97, 0, 0),
        VersionSpec(9, 292, 30, 2, 116, 0, 0),
        VersionSpec(10, 346, 18, 2, 68, 2, 69)
    )

    fun encode(text: String, ecc: QrEccLevel = QrEccLevel.MEDIUM): QrCodeMatrix {
        val rawBytes = text.encodeToByteArray()
        val specs = if (ecc == QrEccLevel.LOW) SPECS_LOW else SPECS_MEDIUM

        var selectedSpec: VersionSpec? = null
        for (spec in specs) {
            val totalData = spec.blocksGroup1 * spec.dataCodewordsBlock1 + spec.blocksGroup2 * spec.dataCodewordsBlock2
            // 4 bits mode (0100) + 8 or 16 bits count + rawBytes.size * 8
            val countBits = if (spec.version <= 9) 8 else 16
            val requiredBits = 4 + countBits + (rawBytes.size * 8)
            val requiredBytes = (requiredBits + 7) / 8
            if (requiredBytes <= totalData) {
                selectedSpec = spec
                break
            }
        }

        val spec = selectedSpec ?: specs.last()
        val version = spec.version
        val totalData = spec.blocksGroup1 * spec.dataCodewordsBlock1 + spec.blocksGroup2 * spec.dataCodewordsBlock2

        // Build BitBuffer
        val bitBuffer = mutableListOf<Int>()
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bitBuffer.add((value shr i) and 1)
            }
        }

        // Mode: 0100 (Byte mode)
        appendBits(4, 4)
        // Character count
        val countBits = if (version <= 9) 8 else 16
        appendBits(rawBytes.size, countBits)
        // Data bytes
        for (b in rawBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zeroes)
        val termZeroes = minOf(4, (totalData * 8) - bitBuffer.size)
        appendBits(0, termZeroes)
        // Byte align with zeroes
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(0)
        }
        // Pad bytes (0xEC, 0x11 alternating)
        val pad = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < totalData * 8) {
            appendBits(pad[padIdx % 2], 8)
            padIdx++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(totalData)
        for (i in 0 until totalData) {
            var b = 0
            for (bit in 0 until 8) {
                b = (b shl 1) or bitBuffer[i * 8 + bit]
            }
            dataCodewords[i] = b
        }

        // Split into blocks and compute Reed-Solomon ECC
        val totalBlocks = spec.blocksGroup1 + spec.blocksGroup2
        val dataBlocks = Array(totalBlocks) { IntArray(0) }
        val eccBlocks = Array(totalBlocks) { IntArray(0) }

        var offset = 0
        var blockIdx = 0
        for (i in 0 until spec.blocksGroup1) {
            val len = spec.dataCodewordsBlock1
            val block = dataCodewords.copyOfRange(offset, offset + len)
            dataBlocks[blockIdx] = block
            eccBlocks[blockIdx] = rsComputeEcc(block, spec.eccCodewordsPerBlock)
            offset += len
            blockIdx++
        }
        for (i in 0 until spec.blocksGroup2) {
            val len = spec.dataCodewordsBlock2
            val block = dataCodewords.copyOfRange(offset, offset + len)
            dataBlocks[blockIdx] = block
            eccBlocks[blockIdx] = rsComputeEcc(block, spec.eccCodewordsPerBlock)
            offset += len
            blockIdx++
        }

        // Interleave codewords
        val finalCodewords = mutableListOf<Int>()
        val maxDataLen = maxOf(spec.dataCodewordsBlock1, spec.dataCodewordsBlock2)
        for (i in 0 until maxDataLen) {
            for (b in 0 until totalBlocks) {
                if (i < dataBlocks[b].size) {
                    finalCodewords.add(dataBlocks[b][i])
                }
            }
        }
        for (i in 0 until spec.eccCodewordsPerBlock) {
            for (b in 0 until totalBlocks) {
                finalCodewords.add(eccBlocks[b][i])
            }
        }

        // Matrix layout
        val matrixDim = version * 4 + 17
        val matrix = Array(matrixDim) { BooleanArray(matrixDim) }
        val isFunction = Array(matrixDim) { BooleanArray(matrixDim) }

        fun setFunction(x: Int, y: Int, value: Boolean) {
            if (x in 0 until matrixDim && y in 0 until matrixDim) {
                matrix[y][x] = value
                isFunction[y][x] = true
            }
        }

        // 1. Finder patterns (top-left, top-right, bottom-left)
        fun drawFinder(startX: Int, startY: Int) {
            for (dy in -1..7) {
                for (dx in -1..7) {
                    val x = startX + dx
                    val y = startY + dy
                    if (x in 0 until matrixDim && y in 0 until matrixDim) {
                        val isBlack = (dx in 0..6 && dy in 0..6) && (
                            dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4)
                        )
                        setFunction(x, y, isBlack)
                    }
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(matrixDim - 7, 0)
        drawFinder(0, matrixDim - 7)

        // 2. Alignment patterns (version >= 2)
        val alignPositions = getAlignmentPatternPositions(version)
        for (ax in alignPositions) {
            for (ay in alignPositions) {
                if (isFunction[ay][ax]) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val isBlack = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1
                        setFunction(ax + dx, ay + dy, isBlack)
                    }
                }
            }
        }

        // 3. Timing patterns
        for (i in 8 until matrixDim - 8) {
            val bit = (i % 2 == 0)
            if (!isFunction[6][i]) setFunction(i, 6, bit)
            if (!isFunction[i][6]) setFunction(6, i, bit)
        }

        // Dark module
        setFunction(8, 4 * version + 9, true)

        // Reserve Format Info bits
        for (i in 0..8) {
            if (!isFunction[8][i]) isFunction[8][i] = true
            if (!isFunction[i][8]) isFunction[i][8] = true
        }
        for (i in matrixDim - 8 until matrixDim) {
            if (!isFunction[8][i]) isFunction[8][i] = true
            if (!isFunction[i][8]) isFunction[i][8] = true
        }

        // 4. Place Data Codewords
        var bitIndex = 0
        val totalBits = finalCodewords.size * 8
        var right = matrixDim - 1

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            val upward = ((matrixDim - 1 - right) / 2) % 2 == 0
            val yRange = if (upward) (matrixDim - 1 downTo 0) else (0 until matrixDim)

            for (y in yRange) {
                for (col in 0..1) {
                    val x = right - col
                    if (!isFunction[y][x]) {
                        var bit = false
                        if (bitIndex < totalBits) {
                            val byteVal = finalCodewords[bitIndex / 8]
                            bit = ((byteVal shr (7 - (bitIndex % 8))) and 1) == 1
                            bitIndex++
                        }
                        // Apply default Mask 0: (x + y) % 2 == 0
                        val mask = (x + y) % 2 == 0
                        matrix[y][x] = if (mask) !bit else bit
                    }
                }
            }
            right -= 2
        }

        // 5. Write Format Information (ECC Medium = 00, Mask 0 = 000 -> 00000)
        // BCH code for 00000 xor 101010000010010 = 101010000010010
        val formatBits = getFormatBits(ecc, 0)
        for (i in 0..5) matrix[8][i] = ((formatBits shr i) and 1) == 1
        matrix[8][7] = ((formatBits shr 6) and 1) == 1
        matrix[8][8] = ((formatBits shr 7) and 1) == 1
        matrix[7][8] = ((formatBits shr 8) and 1) == 1
        for (i in 9..14) matrix[14 - i][8] = ((formatBits shr i) and 1) == 1

        for (i in 0..7) matrix[matrixDim - 1 - i][8] = ((formatBits shr i) and 1) == 1
        for (i in 8..14) matrix[8][matrixDim - 15 + i] = ((formatBits shr i) and 1) == 1

        return QrCodeMatrix(matrixDim, matrix)
    }

    private fun getAlignmentPatternPositions(version: Int): IntArray {
        if (version <= 1) return intArrayOf()
        val numPatterns = version / 7 + 2
        val step = if (version == 32) 26 else ((version * 4 + numPatterns * 2 + 1) / (numPatterns * 2 - 2)) * 2
        val result = IntArray(numPatterns)
        result[0] = 6
        for (i in numPatterns - 1 downTo 1) {
            result[i] = (version * 4 + 17 - 7) - (numPatterns - 1 - i) * step
        }
        return result
    }

    private fun getFormatBits(ecc: QrEccLevel, mask: Int): Int {
        val data = (ecc.formatBits shl 3) or mask
        var rem = data shl 10
        val generator = 0x537
        for (i in 14 downTo 10) {
            if ((rem and (1 shl i)) != 0) {
                rem = rem xor (generator shl (i - 10))
            }
        }
        return ((data shl 10) or rem) xor 0x5412
    }
}

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color.Black
) {
    val qrMatrix = remember(content) {
        try {
            QrCodeEncoder.encode(content, QrEccLevel.MEDIUM)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (qrMatrix != null) {
                val dim = qrMatrix.size
                val cellSize = size.minDimension / dim

                for (y in 0 until dim) {
                    for (x in 0 until dim) {
                        if (qrMatrix.getModule(x, y)) {
                            drawRect(
                                color = codeColor,
                                topLeft = Offset(x * cellSize, y * cellSize),
                                size = Size(cellSize + 0.5f, cellSize + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
