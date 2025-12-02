package com.cs407.tipsytrek.sim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min
import android.view.WindowManager
import android.view.WindowMetrics
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Data classes for rendering
data class ParticleData(
    val x: Float,
    val y: Float,
    val color: Color,
    val r: Short = 0,
    val g: Short = 0,
    val b: Short = 0,
    val a: Short = 255
)

data class BodyData(
    val x: Float,
    val y: Float,
    val angle: Float,
    val type: String, // "box", "circle", "edge"
    val width: Float = 0f,
    val height: Float = 0f,
    val radius: Float = 0f,
    val vertices: List<Pair<Float, Float>> = emptyList()
)

data class SimulationState(
    val particles: List<ParticleData> = emptyList(),
    val bodies: List<BodyData> = emptyList(),
    val particleCount: Int = 0,
    val fps: Int = 0,
    val isPaused: Boolean = false
)

// ViewModel to manage simulation
class PhysicsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private var world: World? = null
    private var particleSystem: ParticleSystem? = null

    private val bodies = mutableMapOf<String, Pair<Body, String>>() // Body to type mapping
    private var simulationJob: Job? = null

    private val targetDt = 1f / 60f
    private var lastFrameTime = System.currentTimeMillis()
    private var frameCount = 0
    private var fpsCounter = 0

    fun initSimulation(type: SimulationType) {
        cleanup()

        world = World(0f, -10f)

        when (type) {
            SimulationType.WATER_DROP -> setupWaterDropSimulation()
            SimulationType.PARTICLE_FOUNTAIN -> setupParticleFountain()
            SimulationType.MIXED_LIQUIDS -> setupMixedLiquids()
            SimulationType.DAM_BREAK -> setupDamBreak()
            SimulationType.DRINK_TEMP -> setupDrink()
        }

        startSimulation()
    }

    private fun deleteOffScreenParticles() {
        world?.let { w ->
            particleSystem?.let { ps ->
                // Create kill zones around the visible area

                // Left kill zone
                deleteParticlesInZone(ps, -50f, 0f, 20f, 80f)

                // Right kill zone
                deleteParticlesInZone(ps, 50f, 0f, 20f, 80f)

                // Bottom kill zone
                deleteParticlesInZone(ps, 0f, -17f, 100f, 20f)

                // Top kill zone (optional)
                //deleteParticlesInZone(ps, 0f, 40f, 100f, 20f)
            }
        }
    }

    private fun deleteParticlesInZone(
        ps: ParticleSystem,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float
    ) {
        // Create a box shape for the kill zone
        val shape = PolygonShape().apply {
            setAsBox(width / 2f, height / 2f)
        }

        // Create a transform for the position
        val transform = Transform()

        // Create a temporary static body at the kill zone position
        val bodyDef = BodyDef().apply {
            type = BodyType.staticBody
            setPosition(centerX, centerY)
        }

        val tempBody = world?.createBody(bodyDef)

        tempBody?.let { body ->
            // Use the body's transform
            ps.destroyParticlesInShape(shape, body.transform)

            // Clean up temporary body
            world?.destroyBody(body)
        }

        // Clean up
        bodyDef.delete()
        shape.delete()
        transform.delete()
    }

    private fun setupWaterDropSimulation() {
        world?.let { w ->
            // Create particle system

            val particleSystemDef = ParticleSystemDef().apply {
                radius = 0.15f
                density = 1.2f
                dampingStrength = 0.2f
                pressureStrength = 0.05f
                viscousStrength = 0.1f
            }
            particleSystem = w.createParticleSystem(particleSystemDef)
            particleSystemDef.delete()

            // Create boundaries
            createBoundaries(w)

            // Create container
            createContainer(w, 0f, -5f, 20f, 10f)

            // Create water drops
            for (i in 0 until 3) {
                createWaterDrop(w, -6f + i * 6f, 15f, 2f)
            }
        }
    }

    private fun setupParticleFountain() {
        world?.let { w ->
            val particleSystemDef = ParticleSystemDef().apply {
                radius = 0.1f
                density = 1.0f
                dampingStrength = 0.3f
            }
            particleSystem = w.createParticleSystem(particleSystemDef)
            particleSystemDef.delete()

            createBoundaries(w)

            // Initial fountain burst
            createParticleFountain(w, 0f, -8f, 150)
        }
    }

    private fun setupMixedLiquids() {
        world?.let { w ->
            val particleSystemDef = ParticleSystemDef().apply {
                radius = 0.12f
                density = 1.0f
                dampingStrength = 0.2f
                viscousStrength = 0.15f
            }
            particleSystem = w.createParticleSystem(particleSystemDef)
            particleSystemDef.delete()

            createBoundaries(w)

            // Left: Water (blue)
            createColoredLiquid(w, -12f, 10f, 6f, 8f, Color.Yellow)

            // Right: Viscous liquid (brown)
            createColoredLiquid(w, 12f, 10f, 6f, 8f, Color.Black)
        }
    }

    private fun setupDamBreak() {
        world?.let { w ->
            val particleSystemDef = ParticleSystemDef().apply {
                radius = 0.15f
                density = 1.2f
                dampingStrength = 0.1f
            }
            particleSystem = w.createParticleSystem(particleSystemDef)
            particleSystemDef.delete()

            createBoundaries(w)

            // Water on left side
            createColoredLiquid(w, -15f, 5f, 8f, 15f, Color.Cyan)

            // Dam in middle
            createDam(w, 0f, 0f, 12f)
        }
    }

    private fun setupDrink() {
        world?.let { w ->
            val particleSystemDef = ParticleSystemDef().apply {
                radius = 0.12f
                density = 1.0f
                dampingStrength = 0.2f
                viscousStrength = 0.15f
            }
            particleSystem = w.createParticleSystem(particleSystemDef)
            particleSystemDef.delete()

            createBoundaries(w)

            createBeerGlass(world = w, x = 0f, y = 10f)
            createColoredLiquid(w, 0f, 60f, 0.5f, 100f, Color(0xFFFFB84D))

        }
    }

    private fun createBoundaries(w: World) {
        // Ground
        val ground = createStaticBody(w, 0f, -10f) {
            val shape = EdgeShape().apply { set(-40f, 0f, 40f, 0f) }
            createFixture(shape, 0f)
            shape.delete()
        }
        bodies["ground"] = ground to "edge"

        // Walls
        val leftWall = createStaticBody(w, -30f, 10f) {
            val shape = EdgeShape().apply { set(0f, -30f, 0f, 30f) }
            createFixture(shape, 0f)
            shape.delete()
        }
        bodies["left_wall"] = leftWall to "edge"

        val rightWall = createStaticBody(w, 30f, 10f) {
            val shape = EdgeShape().apply { set(0f, -30f, 0f, 30f) }
            createFixture(shape, 0f)
            shape.delete()
        }
        bodies["right_wall"] = rightWall to "edge"
    }

    private fun createContainer(w: World, x: Float, y: Float, width: Float, height: Float) {
        // Bottom
        createStaticBody(w, x, y - height / 2f) {
            val shape = EdgeShape().apply { set(-width / 2f, 0f, width / 2f, 0f) }
            createFixture(shape, 0f)
            shape.delete()
        }

        // Left
        createStaticBody(w, x - width / 2f, y) {
            val shape = EdgeShape().apply { set(0f, -height / 2f, 0f, height / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        }

        // Right
        createStaticBody(w, x + width / 2f, y) {
            val shape = EdgeShape().apply { set(0f, -height / 2f, 0f, height / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        }
    }

    private fun createWaterDrop(w: World, x: Float, y: Float, radius: Float) {
        particleSystem?.let { ps ->
            val particleGroupDef = ParticleGroupDef().apply {
                flags = 0L // waterParticle
                position = Vec2(x, y)
                color = ParticleColor(30, 144, 255, 255)

                val shape = CircleShape().apply {
                    this.radius = radius
                    setPosition(0f, 0f)
                }
                this.shape = shape
            }

            ps.createParticleGroup(particleGroupDef)
            particleGroupDef.shape?.delete()
            particleGroupDef.delete()
        }
    }

    private fun createColoredLiquid(w: World, x: Float, y: Float, width: Float, height: Float, color: Color) {
        particleSystem?.let { ps ->
            val particleGroupDef = ParticleGroupDef().apply {
                flags = 0L
                position = Vec2(x, y)
                this.color = ParticleColor(
                    (color.red * 255).toInt().toShort(),
                    (color.green * 255).toInt().toShort(),
                    (color.blue * 255).toInt().toShort(),
                    255
                )

                val shape = PolygonShape().apply {
                    setAsBox(width / 2f, height / 2f)
                }
                this.shape = shape
            }

            ps.createParticleGroup(particleGroupDef)
            particleGroupDef.shape?.delete()
            particleGroupDef.delete()
        }
    }

    private fun createParticleFountain(w: World, x: Float, y: Float, count: Int) {
        particleSystem?.let { ps ->
            val colors = listOf(
                Color.Red, Color.Green, Color.Blue,
                Color.Yellow, Color.Magenta, Color.Cyan
            )

            repeat(count) { i ->
                val angle = (i.toFloat() / count) * 2f * PI.toFloat()
                val speed = 8f

                val particleDef = ParticleDef().apply {
                    flags = 0L
                    position = Vec2(x, y)
                    velocity = Vec2(
                        cos(angle) * speed,
                        sin(angle) * speed + 5f
                    )

                    val color = colors[i % colors.size]
                    this.color = ParticleColor(
                        (color.red * 255).toInt().toShort(),
                        (color.green * 255).toInt().toShort(),
                        (color.blue * 255).toInt().toShort(),
                        255
                    )
                }

                ps.createParticle(particleDef)
                particleDef.delete()
            }
        }
    }

    private fun createDam(w: World, x: Float, y: Float, height: Float) {
        val body = createDynamicBody(w, x, y) {
            val shape = PolygonShape().apply { setAsBox(0.5f, height / 2f) }
            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 2.0f
                friction = 0.5f
            }
            createFixture(fixtureDef)
            shape.delete()
            fixtureDef.delete()
        }
        bodies["dam"] = body to "box"
    }

    // Traditional beer glass (pint glass) - static container
    fun createBeerGlass(world: World, x: Float, y: Float, height: Float = 10f, bottomWidth: Float = 4f, topWidth: Float = 5f): List<Body> {
        val bodies = mutableListOf<Body>()

        // Bottom of glass
        val bottom = createStaticBody(world, x, y - height / 2f) {
            val shape = EdgeShape().apply {
                set(-bottomWidth / 2f, 0f, bottomWidth / 2f, 0f)
            }
            createFixture(shape, 0f)
            shape.delete()
            userData = "glass_bottom"
        }
        bodies.add(bottom)

        // Left wall (angled outward)
        val leftWall = createStaticBody(world, x, y) {
            val shape = EdgeShape().apply {
                set(-bottomWidth / 2f, -height / 2f, -topWidth / 2f, height / 2f)
            }
            createFixture(shape, 0f)
            shape.delete()
            userData = "glass_left"
        }
        bodies.add(leftWall)

        // Right wall (angled outward)
        val rightWall = createStaticBody(world, x, y) {
            val shape = EdgeShape().apply {
                set(bottomWidth / 2f, -height / 2f, topWidth / 2f, height / 2f)
            }
            createFixture(shape, 0f)
            shape.delete()
            userData = "glass_right"
        }
        bodies.add(rightWall)

        return bodies
    }

    // Beer mug with handle (static)
    fun createBeerMug(world: World, x: Float, y: Float, height: Float = 10f, width: Float = 6f): List<Body> {
        val bodies = mutableListOf<Body>()

        // Bottom
        bodies.add(createStaticBody(world, x, y - height / 2f) {
            val shape = EdgeShape().apply { set(-width / 2f, 0f, width / 2f, 0f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Left wall
        bodies.add(createStaticBody(world, x - width / 2f, y) {
            val shape = EdgeShape().apply { set(0f, -height / 2f, 0f, height / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Right wall
        bodies.add(createStaticBody(world, x + width / 2f, y) {
            val shape = EdgeShape().apply { set(0f, -height / 2f, 0f, height / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Handle (decorative circles on the right)
        val handleTop = createStaticBody(world, x + width / 2f + 1f, y + height / 4f) {
            val shape = CircleShape().apply { radius = 0.3f }
            createFixture(shape, 0f)
            shape.delete()
        }
        bodies.add(handleTop)

        val handleBottom = createStaticBody(world, x + width / 2f + 1f, y - height / 4f) {
            val shape = CircleShape().apply { radius = 0.3f }
            createFixture(shape, 0f)
            shape.delete()
        }
        bodies.add(handleBottom)

        return bodies
    }

    // Wine glass shape (narrow stem, wider top)
    fun createWineGlass(world: World, x: Float, y: Float): List<Body> {
        val bodies = mutableListOf<Body>()

        val cupHeight = 6f
        val cupWidth = 4f
        val stemHeight = 4f
        val stemWidth = 0.4f
        val baseWidth = 3f

        // Bowl of wine glass - left side
        bodies.add(createStaticBody(world, x, y) {
            val shape = EdgeShape().apply {
                set(-stemWidth / 2f, -stemHeight, -cupWidth / 2f, cupHeight - stemHeight)
            }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Bowl of wine glass - right side
        bodies.add(createStaticBody(world, x, y) {
            val shape = EdgeShape().apply {
                set(stemWidth / 2f, -stemHeight, cupWidth / 2f, cupHeight - stemHeight)
            }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Stem - left
        bodies.add(createStaticBody(world, x - stemWidth / 2f, y - stemHeight / 2f) {
            val shape = EdgeShape().apply { set(0f, -stemHeight / 2f, 0f, stemHeight / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Stem - right
        bodies.add(createStaticBody(world, x + stemWidth / 2f, y - stemHeight / 2f) {
            val shape = EdgeShape().apply { set(0f, -stemHeight / 2f, 0f, stemHeight / 2f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Base
        bodies.add(createStaticBody(world, x, y - stemHeight) {
            val shape = EdgeShape().apply { set(-baseWidth / 2f, 0f, baseWidth / 2f, 0f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        return bodies
    }

    // Curved beer glass using multiple segments
    fun createCurvedBeerGlass(world: World, x: Float, y: Float, height: Float = 12f, segments: Int = 10): List<Body> {
        val bodies = mutableListOf<Body>()
        val bottomRadius = 2f
        val topRadius = 2.5f

        // Bottom
        bodies.add(createStaticBody(world, x, y - height / 2f) {
            val shape = EdgeShape().apply { set(-bottomRadius, 0f, bottomRadius, 0f) }
            createFixture(shape, 0f)
            shape.delete()
        })

        // Create curved walls using multiple line segments
        val segmentHeight = height / segments

        // Left wall segments
        for (i in 0 until segments) {
            val t1 = i.toFloat() / segments
            val t2 = (i + 1).toFloat() / segments

            val y1 = y - height / 2f + i * segmentHeight
            val y2 = y - height / 2f + (i + 1) * segmentHeight

            // Slight curve - wider at top
            val x1 = -(bottomRadius + (topRadius - bottomRadius) * t1)
            val x2 = -(bottomRadius + (topRadius - bottomRadius) * t2)

            bodies.add(createStaticBody(world, x, 0f) {
                val shape = EdgeShape().apply { set(x1, y1, x2, y2) }
                createFixture(shape, 0f)
                shape.delete()
            })
        }

        // Right wall segments
        for (i in 0 until segments) {
            val t1 = i.toFloat() / segments
            val t2 = (i + 1).toFloat() / segments

            val y1 = y - height / 2f + i * segmentHeight
            val y2 = y - height / 2f + (i + 1) * segmentHeight

            val x1 = bottomRadius + (topRadius - bottomRadius) * t1
            val x2 = bottomRadius + (topRadius - bottomRadius) * t2

            bodies.add(createStaticBody(world, x, 0f) {
                val shape = EdgeShape().apply { set(x1, y1, x2, y2) }
                createFixture(shape, 0f)
                shape.delete()
            })
        }

        return bodies
    }

    private fun createStaticBody(w: World, x: Float, y: Float, init: Body.() -> Unit): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.staticBody
            setPosition(x, y)
        }
        val body = w.createBody(bodyDef)
        bodyDef.delete()
        body.init()
        return body
    }

    private fun createDynamicBody(w: World, x: Float, y: Float, init: Body.() -> Unit): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.dynamicBody
            setPosition(x, y)
        }
        val body = w.createBody(bodyDef)
        bodyDef.delete()
        body.init()
        return body
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive) {
                if (!_state.value.isPaused) {
                    updateSimulation()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private fun updateSimulation() {
        world?.let { w ->
            // Step physics
            w.step(targetDt, 8, 3,
                liquidfun.b2CalculateParticleIterations(10f, 0.15f, targetDt))
            //Delete off-screen particles
            deleteOffScreenParticles()
            // Extract particle data
            val particles = mutableListOf<ParticleData>()
            particleSystem?.let { ps ->
                val count = ps.particleCount

                if (count > 0) {
                    // Allocate buffer for colors (4 bytes per particle: R, G, B, A)
                    val colorBufferSize = count * 4
                    val colorBuffer = ByteBuffer.allocateDirect(colorBufferSize)
                        .order(ByteOrder.nativeOrder())

                    // Copy color data from particle system
                    val copiedColors = ps.copyColorBuffer(0, count, colorBuffer)

                    if (copiedColors > 0) {
                        colorBuffer.rewind()

                        for (i in 0 until count) {
                            val x = ps.getParticlePositionX(i)
                            val y = ps.getParticlePositionY(i)

                            // Read RGBA values (unsigned bytes 0-255)
                            val r = colorBuffer.get().toInt() and 0xFF
                            val g = colorBuffer.get().toInt() and 0xFF
                            val b = colorBuffer.get().toInt() and 0xFF
                            val a = colorBuffer.get().toInt() and 0xFF

                            // Convert to Compose Color
                            val particleColor = Color(r, g, b, a)

                            particles.add(
                                ParticleData(
                                    x = x,
                                    y = y,
                                    color = particleColor
                                )
                            )
                        }
                    } else {
                        // Fallback: if color buffer fails, use default color
                        for (i in 0 until count) {
                            particles.add(
                                ParticleData(
                                    ps.getParticlePositionX(i),
                                    ps.getParticlePositionY(i),
                                    Color.Cyan
                                )
                            )
                        }
                    }
                }
            }

            _state.value = _state.value.copy(
                particles = particles,
                particleCount = particles.size
            )

            // Extract body data
            val bodyDataList = bodies.mapNotNull { (_, pair) ->
                val (body, type) = pair
                when (type) {
                    "box" -> {
                        // Get box dimensions from fixture (simplified - assumes first fixture)
                        BodyData(
                            x = body.positionX,
                            y = body.positionY,
                            angle = body.angle,
                            type = "box",
                            width = 1f, // Would need to extract from shape
                            height = 5f
                        )
                    }
                    "circle" -> {
                        BodyData(
                            x = body.positionX,
                            y = body.positionY,
                            angle = body.angle,
                            type = "circle",
                            radius = 0.8f // Would need to extract from shape
                        )
                    }
                    else -> null
                }
            }

            // Calculate FPS
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFrameTime >= 1000) {
                fpsCounter = frameCount
                frameCount = 0
                lastFrameTime = currentTime
            }

            _state.value = SimulationState(
                particles = particles,
                bodies = bodyDataList,
                particleCount = particles.size,
                fps = fpsCounter,
                isPaused = _state.value.isPaused
            )
        }
    }

    fun togglePause() {
        _state.value = _state.value.copy(isPaused = !_state.value.isPaused)
    }

    fun addWaterDrop(x: Float, y: Float) {
        world?.let { w ->
            createWaterDrop(w, x, y, 1.5f)
        }
    }

    fun addExplosion(x: Float, y: Float) {
        bodies.values.forEach { (body, _) ->
            if (body.type == BodyType.dynamicBody) {
                val dx = body.positionX - x
                val dy = body.positionY - y
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < 10f && distance > 0.1f) {
                    val force = 500f * (1f - distance / 10f)
                    body.applyLinearImpulse(
                        Vec2((dx / distance) * force, (dy / distance) * force),
                        body.worldCenter,
                        true
                    )
                }
            }
        }
    }

    private fun cleanup() {
        simulationJob?.cancel()
        world?.delete()
        world = null
        particleSystem = null
        bodies.clear()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

enum class SimulationType {
    WATER_DROP,
    PARTICLE_FOUNTAIN,
    MIXED_LIQUIDS,
    DAM_BREAK,
    DRINK_TEMP
}

// Composable for rendering the simulation
@Composable
fun PhysicsSimulationCanvas(
    state: SimulationState,
    modifier: Modifier = Modifier,
    onTap: (Offset) -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .requiredSize(3000.dp)
            .background(Color(0x00000000))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset)
                }
            }
    ) {
        val worldWidth = 60f  // World units
        val worldHeight = 40f // World units

        // Calculate scale to fit world in canvas
        val scaleX = size.width / worldWidth
        val scaleY = size.height / worldHeight
        val scale = min(scaleX, scaleY)

        // Center offset
        val offsetX = (size.width - worldWidth * scale) / 2f
        val offsetY = size.height - (worldHeight * scale) / 2f

        // Helper function to convert world to screen coordinates
        fun worldToScreen(wx: Float, wy: Float): Offset {
            return Offset(
                wx * scale + offsetX + (worldWidth * scale) / 2f,
                offsetY - wy * scale
            )
        }

        // Draw particles
        state.particles.forEach { particle ->
            val screenPos = worldToScreen(particle.x, particle.y)
            drawCircle(
                color = particle.color,
                radius = 0.15f * scale,
                center = screenPos,
                alpha = 0.8f
            )
        }

        // Draw bodies
        state.bodies.forEach { body ->
            when (body.type) {
                "glass" -> {
                    val center = worldToScreen(body.x, body.y)
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(
                            center.x - body.width * scale,
                            center.y - body.height * scale
                        ),
                        size = Size(body.width * scale * 2f, body.height * scale * 2f)
                    )
                }
                "box" -> {
                    val center = worldToScreen(body.x, body.y)
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(
                            center.x - body.width * scale,
                            center.y - body.height * scale
                        ),
                        size = Size(body.width * scale * 2f, body.height * scale * 2f)
                    )
                }
                "circle" -> {
                    val center = worldToScreen(body.x, body.y)
                    drawCircle(
                        color = Color(0xFFFF6B6B),
                        radius = body.radius * scale,
                        center = center
                    )

                    // Draw line to show rotation
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(
                            center.x + cos(body.angle) * body.radius * scale,
                            center.y + sin(body.angle) * body.radius * scale
                        ),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Draw grid
        drawGrid(worldWidth, worldHeight, scale, offsetX, offsetY)
    }
}

private fun DrawScope.drawGrid(
    worldWidth: Float,
    worldHeight: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val gridColor = Color.Black.copy(alpha = 0.1f)
    val gridSize = 5f // 5 world units

    // Vertical lines
    for (x in -30..30 step gridSize.toInt()) {
        val screenX = x * scale + offsetX + (worldWidth * scale) / 2f
        drawLine(
            color = gridColor,
            start = Offset(screenX, 0f),
            end = Offset(screenX, size.height),
            strokeWidth = 1f
        )
    }

    // Horizontal lines
    for (y in -20..20 step gridSize.toInt()) {
        val screenY = offsetY - y * scale
        drawLine(
            color = gridColor,
            start = Offset(0f, screenY),
            end = Offset(size.width, screenY),
            strokeWidth = 1f
        )
    }
}

// Main UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsSimulationScreen(
    viewModel: PhysicsViewModel = viewModel(),
    color: Color = Color(0xFFFFB84D),
    onDrink: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { viewModel.initSimulation(SimulationType.DRINK_TEMP) },
                            modifier = Modifier.fillMaxWidth(0.33f)
                            ) {
                            Text("Drink", fontSize = 12.sp)
                        }
                    }


                        },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x00000000)
                ),
            )

        },
        bottomBar = {
            /*SimulationControls(
                state = state,
                onSimulationTypeSelected = { type ->
                    viewModel.initSimulation(type)
                },
                onTogglePause = { viewModel.togglePause() }
            )*/
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PhysicsSimulationCanvas(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTap = { offset ->
                    // Convert screen tap to world coordinates (simplified)
                    val worldX = (offset.x / 20f) - 15f
                    val worldY = 20f - (offset.y / 20f)
                    viewModel.addWaterDrop(worldX, worldY)
                }
            )

            // Stats overlay
            SimulationStats(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun SimulationControls(
    state: SimulationState,
    onSimulationTypeSelected: (SimulationType) -> Unit,
    onTogglePause: () -> Unit
) {
    Surface(
        color = Color(0xFF16213E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSimulationTypeSelected(SimulationType.WATER_DROP) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Water", fontSize = 12.sp)
                }
                Button(
                    onClick = { onSimulationTypeSelected(SimulationType.PARTICLE_FOUNTAIN) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fountain", fontSize = 12.sp)
                }
                Button(
                    onClick = { onSimulationTypeSelected(SimulationType.MIXED_LIQUIDS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mixed", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSimulationTypeSelected(SimulationType.DAM_BREAK) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dam", fontSize = 12.sp)
                }
                Button(
                    onClick = { onSimulationTypeSelected(SimulationType.DRINK_TEMP) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Balls", fontSize = 12.sp)
                }
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isPaused) Color.Green else Color.Red
                    )
                ) {
                    Text(if (state.isPaused) "Resume" else "Pause", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SimulationStats(
    state: SimulationState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "FPS: ${state.fps}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Particles: ${state.particleCount}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Bodies: ${state.bodies.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            if (state.isPaused) {
                Text(
                    text = "⏸ PAUSED",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}