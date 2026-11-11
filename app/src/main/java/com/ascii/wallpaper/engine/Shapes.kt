package com.ascii.wallpaper.engine

import com.ascii.wallpaper.data.ShapeType
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Flat, allocation-free storage for the shape point cloud. */
internal class PointCloud(capacity: Int) {
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val z = FloatArray(capacity)
    val shade = FloatArray(capacity)

    var size: Int = 0
        private set

    fun add(px: Float, py: Float, pz: Float, brightness: Float) {
        if (size >= x.size) return
        x[size] = px
        y[size] = py
        z[size] = pz
        shade[size] = brightness
        size++
    }
}

/**
 * Procedural geometry for every figure.
 *
 * All shapes are generated once (per shape/density/thickness change) into a
 * [PointCloud]; the renderer then only rotates/projects it, which keeps the
 * per-frame cost predictable even at 120 fps.
 */
internal object ShapeFactory {

    private const val TAU = (PI * 2.0).toFloat()

    /** Cube corners are indexed by bits (x=4, y=2, z=1); an edge flips one bit. */
    private val EDGE_BITS = intArrayOf(1, 2, 4)

    fun build(shape: ShapeType, glyphCount: Int, thickness: Float, seed: Long): PointCloud {
        val capacity = glyphCount.coerceIn(120, 2600)
        val cloud = PointCloud(capacity)
        val random = Random(seed)
        when (shape) {
            ShapeType.PASTA -> pasta(cloud, capacity, thickness, random)
            ShapeType.CUBES -> cubes(cloud, capacity, thickness, random)
            ShapeType.BIG_CUBE -> bigCube(cloud, capacity, thickness, random)
            ShapeType.DNA -> dna(cloud, capacity, thickness, random)
            ShapeType.BLACK_HOLE -> blackHole(cloud, capacity, thickness, random)
        }
        return cloud
    }

    // ---------------------------------------------------------------- pasta --

    /** Three tangled torus knots = a bowl of glowing spaghetti. */
    private fun pasta(pc: PointCloud, count: Int, thickness: Float, random: Random) {
        val strands = 3
        val perStrand = count / strands
        if (perStrand <= 0) return
        for (s in 0 until strands) {
            val phase = s * TAU / strands
            val q = if (s % 2 == 0) 2 else 3
            val tube = 0.10f * thickness
            for (i in 0 until perStrand) {
                val t = TAU * i / perStrand
                val r = 2.0 + cos(q * t)
                var px = (r * cos(3 * t + phase) * 0.30).toFloat()
                var py = (r * sin(3 * t + phase) * 0.30).toFloat()
                var pz = (sin(q * t) * 0.78).toFloat()
                px += random.nextGaussian().toFloat() * tube
                py += random.nextGaussian().toFloat() * tube
                pz += random.nextGaussian().toFloat() * tube
                val shade = (0.55f + 0.45f * (i.toFloat() / perStrand)).coerceIn(0.2f, 1f)
                pc.add(px, py, pz, shade)
            }
        }
    }

    // ---------------------------------------------------------------- cubes --

    /** A drifting cluster of small wireframe cubes. */
    private fun cubes(pc: PointCloud, count: Int, thickness: Float, random: Random) {
        val positions = listOf(
            floatArrayOf(-0.78f, -0.52f, 0.15f),
            floatArrayOf(0.72f, -0.60f, -0.05f),
            floatArrayOf(-0.10f, 0.62f, 0.35f),
            floatArrayOf(0.55f, 0.48f, -0.42f),
            floatArrayOf(-0.62f, 0.12f, -0.50f),
            floatArrayOf(0.15f, -0.15f, 0.10f),
            floatArrayOf(0.86f, 0.05f, 0.55f),
        )
        val perCube = count / positions.size
        if (perCube < 12) return
        positions.forEachIndexed { index, p ->
            val half = 0.26f + 0.10f * (index % 3)
            val rotX = random.nextFloat() * TAU
            val rotY = random.nextFloat() * TAU
            addCube(
                pc = pc,
                cx = p[0], cy = p[1], cz = p[2],
                half = half,
                rotX = rotX,
                rotY = rotY,
                perEdge = (perCube / 12).coerceAtLeast(2),
                shade = 0.82f,
                thickness = thickness,
                random = random,
            )
        }
    }

    /** One large cube with a faint inner cube for depth. */
    private fun bigCube(pc: PointCloud, count: Int, thickness: Float, random: Random) {
        val outer = (count * 0.78f).toInt()
        addCube(
            pc = pc,
            cx = 0f, cy = 0f, cz = 0f,
            half = 0.98f,
            rotX = 0.42f,
            rotY = 0.55f,
            perEdge = (outer / 12).coerceAtLeast(3),
            shade = 1f,
            thickness = thickness,
            random = random,
        )
        val inner = count - pc.size
        if (inner > 24) {
            addCube(
                pc = pc,
                cx = 0f, cy = 0f, cz = 0f,
                half = 0.42f,
                rotX = 0.42f,
                rotY = 0.55f,
                perEdge = (inner / 12).coerceAtLeast(1),
                shade = 0.5f,
                thickness = thickness,
                random = random,
            )
        }
    }

    /**
     * Adds the 12 edges of a rotated cube. [perEdge] points are placed on every
     * edge; [thickness] adds a small tube radius so the wireframe reads as a
     * tube rather than a hairline.
     */
    private fun addCube(
        pc: PointCloud,
        cx: Float, cy: Float, cz: Float,
        half: Float,
        rotX: Float,
        rotY: Float,
        perEdge: Int,
        shade: Float,
        thickness: Float,
        random: Random,
    ) {
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        val corners = FloatArray(24)
        var index = 0
        var ix = 0
        while (ix < 2) {
            var iy = 0
            while (iy < 2) {
                var iz = 0
                while (iz < 2) {
                    var px = (ix * 2 - 1) * half
                    var py = (iy * 2 - 1) * half
                    var pz = (iz * 2 - 1) * half

                    // rotate around Y, then X
                    val rx = px * cosY + pz * sinY
                    val rz = -px * sinY + pz * cosY
                    val ry = py * cosX - rz * sinX
                    val rz2 = py * sinX + rz * cosX

                    corners[index++] = cx + rx
                    corners[index++] = cy + ry
                    corners[index++] = cz + rz2
                    iz++
                }
                iy++
            }
            ix++
        }

        val jitter = 0.006f + 0.012f * thickness
        for (corner in 0 until 8) {
            for (bit in EDGE_BITS) {
                val other = corner xor bit
                if (other < corner) continue
                val ax = corners[corner * 3]
                val ay = corners[corner * 3 + 1]
                val az = corners[corner * 3 + 2]
                val bx = corners[other * 3]
                val by = corners[other * 3 + 1]
                val bz = corners[other * 3 + 2]
                for (step in 0 until perEdge) {
                    val t = step.toFloat() / perEdge
                    val px = ax + (bx - ax) * t + (random.nextFloat() - 0.5f) * jitter
                    val py = ay + (by - ay) * t + (random.nextFloat() - 0.5f) * jitter
                    val pz = az + (bz - az) * t + (random.nextFloat() - 0.5f) * jitter
                    pc.add(px, py, pz, shade)
                }
            }
        }
    }

    // ------------------------------------------------------------------ dna --

    /** Double helix with connecting base pairs. */
    private fun dna(pc: PointCloud, count: Int, thickness: Float, random: Random) {
        val turns = 2.35f
        val height = 2.35f
        val radius = 0.54f
        val backbonePoints = (count * 0.66f).toInt()
        val perStrand = backbonePoints / 2
        if (perStrand <= 1) return
        val tube = 0.045f * thickness

        for (strand in 0 until 2) {
            val phase = if (strand == 0) 0.0 else PI
            for (i in 0 until perStrand) {
                val t = i.toFloat() / (perStrand - 1)
                val angle = t * turns * TAU + phase
                val y = -height / 2f + t * height
                var px = (cos(angle) * radius).toFloat()
                var pz = (sin(angle) * radius).toFloat() + 0.0f
                var py = y
                px += random.nextGaussian().toFloat() * tube
                py += random.nextGaussian().toFloat() * tube
                pz += random.nextGaussian().toFloat() * tube
                pc.add(px, py, pz, 0.95f)
            }
        }

        val rungPoints = count - backbonePoints
        val rungs = 24
        val perRung = rungPoints / rungs
        if (perRung <= 0) return
        for (rung in 0 until rungs) {
            val t = rung / (rungs - 1f)
            val angle = t * turns * TAU
            val y = -height / 2f + t * height
            val ax = (cos(angle) * radius).toFloat()
            val az = (sin(angle) * radius).toFloat()
            val bx = -ax
            val bz = -az
            for (step in 0 until perRung) {
                val f = step.toFloat() / perRung
                val px = ax + (bx - ax) * f
                val pz = az + (bz - az) * f
                val py = y + (random.nextFloat() - 0.5f) * 0.012f * thickness
                pc.add(px, py, pz, 0.55f)
            }
        }
    }

    // ----------------------------------------------------------- black hole --

    /** Accretion disk + photon ring with an empty event horizon. */
    private fun blackHole(pc: PointCloud, count: Int, thickness: Float, random: Random) {
        val diskPoints = (count * 0.84f).toInt()
        if (diskPoints <= 0) return
        for (i in 0 until diskPoints) {
            val u = random.nextFloat()
            val r = 0.62f + sqrt(u) * 1.02f
            val angle = random.nextFloat() * TAU + r * 2.1f
            val x = (cos(angle) * r).toFloat()
            val z = (sin(angle) * r).toFloat()
            val y = (random.nextGaussian() * 0.012f * (2.2f - thickness)).toFloat()
            val shade = (1.18f - r * 0.42f).coerceIn(0.18f, 1f)
            pc.add(x, y, z, shade)
        }

        val ringPoints = count - diskPoints
        for (i in 0 until ringPoints) {
            val angle = TAU * i / ringPoints
            val x = (cos(angle) * 0.58f).toFloat()
            val z = (sin(angle) * 0.58f).toFloat()
            val y = (random.nextGaussian() * 0.01f).toFloat()
            pc.add(x, y, z, 1f)
        }
    }
}
