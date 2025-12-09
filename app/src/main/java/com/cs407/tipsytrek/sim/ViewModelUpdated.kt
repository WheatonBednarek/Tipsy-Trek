package com.cs407.tipsytrek.sim

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.tipsytrek.Beverage
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.*
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.*
import org.jbox2d.common.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

//TODO Beverage Input
//TODO once particles reach 0, drink has been consumed
//TODO Tweak simulation to request/as needed

// Data classes for rendering
data class ParticleData(
    val x: Float,
    val y: Float,
    val color: Color
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

// Custom particle class to simulate liquid particles
data class CustomParticle(
    val body: Body,
    val color: Color,
    var supportLevel: Int = 0
)

// ViewModel to manage simulation
class PhysicsViewModel(
    private val beverageType: Int = 1,
    private val beverageColor: Color = Color(0xFFFFB84D),
    private val cbF: () -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private var world: World? = null
    private var lastTimestamp: Long = 0L
    private val particles = mutableListOf<CustomParticle>()
    private val bodies = mutableMapOf<String, Pair<Body, String>>() // Body to type mapping
    private var simulationJob: Job? = null

    private val targetDt = 1f / 60f
    private var lastFrameTime = System.currentTimeMillis()
    private var frameCount = 0
    private var fpsCounter = 0
    private var drinkComplete = false
    private var hasPoured = false
    private var hasDrank = false
    fun initSimulation(type: SimulationType) {
        cleanup()

        world = World(Vec2(0f, -10f))
        //world?.gravity = Vec2(0f, -10f)
        //
        when (type) {
            SimulationType.WATER_DROP -> setupWaterDropSimulation()
            SimulationType.PARTICLE_FOUNTAIN -> setupParticleFountain()
            SimulationType.MIXED_LIQUIDS -> setupMixedLiquids()
            SimulationType.DAM_BREAK -> setupDamBreak()
            SimulationType.DRINK_TEMP -> setupDrink()
        }

        if(!drinkComplete) hasPoured = true
        startSimulation()
    }

    private fun deleteOffScreenParticles() {
        world?.let { w ->
            particles.removeAll { particle ->
                val pos = particle.body.position
                val shouldDelete = pos.x < -25f || pos.x > 25f || pos.y < -25f || pos.y > 40f
                if (shouldDelete) {
                    w.destroyBody(particle.body)
                }
                shouldDelete
            }
        }
    }

    private fun setupWaterDropSimulation() {
        world?.let { w ->
            createBoundaries(w)
            createContainer(w, 0f, -5f, 20f, 10f)

            // Create water drops with particles
            for (i in 0 until 3) {
                createWaterDrop(w, -6f + i * 6f, 15f, 2f, Color(0xFF1E90FF))
            }
        }
    }

    private fun setupParticleFountain() {
        world?.let { w ->
            createBoundaries(w)
            createParticleFountain(w, 0f, -8f, 150)
        }
    }

    private fun setupMixedLiquids() {
        world?.let { w ->
            createBoundaries(w)

            // Left: Yellow liquid
            createColoredLiquid(w, -12f, 10f, 6f, 8f, Color.Yellow)

            // Right: Black liquid
            createColoredLiquid(w, 12f, 10f, 6f, 8f, Color.Black)
        }
    }

    private fun setupDamBreak() {
        world?.let { w ->
            createBoundaries(w)

            // Water on left side
            createColoredLiquid(w, -15f, 5f, 8f, 15f, Color.Cyan)

            // Dam in middle
            createDam(w, 0f, 0f, 12f)
        }
    }

    private fun setupDrink() {
        world?.let { w ->
            //createBoundaries(w)
            createBeerGlass(world = w, x = 0f, y = 10f)

            // Pour liquid from above
            createColoredLiquid(w, 0f, 15f, 3f, 12f, beverageColor)
        }
    }

    private fun createBoundaries(w: World) {
        // Ground
        val groundDef = BodyDef().apply {
            position.set(0f, -10f)
        }
        val ground = w.createBody(groundDef)
        val groundShape = EdgeShape().apply {
            set(Vec2(-40f, 0f), Vec2(40f, 0f))
        }
        ground.createFixture(groundShape, 0f)
        bodies["ground"] = ground to "edge"

        // Left Wall
        val leftWallDef = BodyDef().apply {
            position.set(-30f, 10f)
        }
        val leftWall = w.createBody(leftWallDef)
        val leftShape = EdgeShape().apply {
            set(Vec2(0f, -30f), Vec2(0f, 30f))
        }
        leftWall.createFixture(leftShape, 0f)
        bodies["left_wall"] = leftWall to "edge"

        // Right Wall
        val rightWallDef = BodyDef().apply {
            position.set(30f, 10f)
        }
        val rightWall = w.createBody(rightWallDef)
        val rightShape = EdgeShape().apply {
            set(Vec2(0f, -30f), Vec2(0f, 30f))
        }
        rightWall.createFixture(rightShape, 0f)
        bodies["right_wall"] = rightWall to "edge"
    }

    private fun createContainer(w: World, x: Float, y: Float, width: Float, height: Float) {
        // Bottom
        val bottomDef = BodyDef().apply { position.set(x, y - height / 2f) }
        val bottom = w.createBody(bottomDef)
        val bottomShape = EdgeShape().apply {
            set(Vec2(-width / 2f, 0f), Vec2(width / 2f, 0f))
        }
        bottom.createFixture(bottomShape, 0f)

        // Left
        val leftDef = BodyDef().apply { position.set(x - width / 2f, y) }
        val left = w.createBody(leftDef)
        val leftShape = EdgeShape().apply {
            set(Vec2(0f, -height / 2f), Vec2(0f, height / 2f))
        }
        left.createFixture(leftShape, 0f)

        // Right
        val rightDef = BodyDef().apply { position.set(x + width / 2f, y) }
        val right = w.createBody(rightDef)
        val rightShape = EdgeShape().apply {
            set(Vec2(0f, -height / 2f), Vec2(0f, height / 2f))
        }
        right.createFixture(rightShape, 0f)
    }

    private fun createWaterDrop(w: World, x: Float, y: Float, radius: Float, color: Color) {
        // Create multiple small particles to simulate a water drop
        val particleRadius = 0.15f
        val particlesPerRow = (radius / particleRadius).toInt()

        for (i in -particlesPerRow..particlesPerRow) {
            for (j in -particlesPerRow..particlesPerRow) {
                val px = x + i * particleRadius * 2f
                val py = y + j * particleRadius * 2f
                val dx = px - x
                val dy = py - y

                if (sqrt(dx * dx + dy * dy) <= radius) {
                    createParticle(w, px, py, particleRadius, color)
                }
            }
        }
    }

    private fun createColoredLiquid(w: World, x: Float, y: Float, width: Float, height: Float, color: Color) {
        val particleRadius = 0.20f
        val rows = (height / (particleRadius * 2f)).toInt()
        val cols = (width / (particleRadius * 2f)).toInt()

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val px = x - width / 2f + j * particleRadius * 2f + particleRadius
                val py = y - height / 2f + i * particleRadius * 2f + particleRadius
                createParticle(w, px, py, particleRadius, color)
            }
        }
    }

    private fun createParticle(w: World, x: Float, y: Float, radius: Float, color: Color) {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(x, y)
        }
        val body = w.createBody(bodyDef)

        val shape = CircleShape().apply {
            this.radius = radius
        }

        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1.0f
            friction = 0.1f
            restitution = 0.1f
        }

        body.createFixture(fixtureDef)
        particles.add(CustomParticle(body, color))
    }

    private fun createParticleFountain(w: World, x: Float, y: Float, count: Int) {
        val colors = listOf(
            Color.Red, Color.Green, Color.Blue,
            Color.Yellow, Color.Magenta, Color.Cyan
        )

        repeat(count) { i ->
            val angle = (i.toFloat() / count) * 2f * PI.toFloat()
            val speed = 8f
            val color = colors[i % colors.size]

            val bodyDef = BodyDef().apply {
                type = BodyType.DYNAMIC
                position.set(x, y)
            }
            val body = w.createBody(bodyDef)

            val shape = CircleShape().apply {
                radius = 0.1f
            }

            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = 1.0f
                friction = 0.3f
            }

            body.createFixture(fixtureDef)
            body.linearVelocity = Vec2(
                cos(angle) * speed,
                sin(angle) * speed + 5f
            )

            particles.add(CustomParticle(body, color))
        }
    }

    private fun createDam(w: World, x: Float, y: Float, height: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(x, y)
        }
        val body = w.createBody(bodyDef)

        val shape = PolygonShape().apply {
            setAsBox(0.5f, height / 2f)
        }

        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 2.0f
            friction = 0.5f
        }

        body.createFixture(fixtureDef)
        bodies["dam"] = body to "box"
    }

    private fun createBeerGlass(world: World, x: Float, y: Float, height: Float = 10f, bottomWidth: Float = 4f, topWidth: Float = 5f) {
        // Bottom of glass
        val bottomDef = BodyDef().apply { position.set(x, y - height / 2f) }
        val bottom = world.createBody(bottomDef)
        val bottomShape = EdgeShape().apply {
            set(Vec2(-bottomWidth / 2f, 0f), Vec2(bottomWidth / 2f, 0f))
        }
        bottom.createFixture(bottomShape, 0f)

        // Left wall (angled outward)
        val leftDef = BodyDef().apply { position.set(x, y) }
        val leftWall = world.createBody(leftDef)
        val leftShape = EdgeShape().apply {
            set(Vec2(-bottomWidth / 2f, -height / 2f), Vec2(-topWidth / 2f, height / 2f))
        }
        leftWall.createFixture(leftShape, 0f)

        // Right wall (angled outward)
        val rightDef = BodyDef().apply { position.set(x, y) }
        val rightWall = world.createBody(rightDef)
        val rightShape = EdgeShape().apply {
            set(Vec2(bottomWidth / 2f, -height / 2f), Vec2(topWidth / 2f, height / 2f))
        }
        rightWall.createFixture(rightShape, 0f)
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
            w.step(targetDt, 8, 3)
            // Delete off-screen particles
            deleteOffScreenParticles()
            hasDrank = particles.isEmpty()
            if(hasDrank && hasPoured && !drinkComplete)  {
                drinkComplete = true
                cbF()
            }

            // Update support levels every 5 frames

            if (beverageType == 1 && frameCount % 5 == 0) {
                updateParticleSupportLevels()
            }

            // Extract particle data
            val particleDataList = particles.map { particle ->
                val pos = particle.body.position
                val color = when {
                    beverageType != 1 -> beverageColor
                    particle.supportLevel != 0 -> beverageColor
                    else -> Color(0xFFFFFFFF)
                }
                ParticleData(pos.x, pos.y, color)
            }

            // Extract body data
            val bodyDataList = bodies.mapNotNull { (_, pair) ->
                val (body, type) = pair
                when (type) {
                    "box" -> {
                        BodyData(
                            x = body.position.x,
                            y = body.position.y,
                            angle = body.angle,
                            type = "box",
                            width = 1f,
                            height = 5f
                        )
                    }
                    "circle" -> {
                        BodyData(
                            x = body.position.x,
                            y = body.position.y,
                            angle = body.angle,
                            type = "circle",
                            radius = 0.8f
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
                particles = particleDataList,
                bodies = bodyDataList,
                particleCount = particles.size,
                fps = fpsCounter,
                isPaused = _state.value.isPaused
            )
        }
    }

    private fun updateParticleSupportLevels() {
        //val detectionRadius = 0.4f
        //val searchRadius = 0.3f

        particles.forEach { particle ->
            val pos = particle.body.position
            var totalBelow = 0
            particles.forEach { other ->
                if (particle != other) {
                    val otherPos = other.body.position
                    if (otherPos.y > pos.y+0.5) totalBelow++
                }
            }

            particle.supportLevel = if (totalBelow <= 20) 0 else 1
        }
    }

    fun onSensorDataChanged(event: SensorEvent) {
        // Ensure ball is initialized
        world?: return

        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            if (lastTimestamp != 0L) {
                //val NS2S = 1.0f / 1000000000.0f
                //val dT = (event.timestamp - lastTimestamp) * NS2S

                //change math to better reflect?
                //may be fine because gravity is acceleration based
                world!!.gravity = Vec2(-event.values[0], -event.values[1])

            }

            lastTimestamp = event.timestamp
        }
    }

    fun togglePause() {
        _state.value = _state.value.copy(isPaused = !_state.value.isPaused)
    }

    fun addWaterDrop(x: Float, y: Float) {
        world?.let { w ->
            createWaterDrop(w, x, y, 1.5f, Color(0xFF1E90FF))
        }
    }

    fun addExplosion(x: Float, y: Float) {
        bodies.values.forEach { (body, _) ->
            if (body.type == BodyType.DYNAMIC) {
                val pos = body.position
                val dx = pos.x - x
                val dy = pos.y - y
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < 10f && distance > 0.1f) {
                    val force = 500f * (1f - distance / 10f)
                    /*body.applyLinearImpulse(
                        Vec2((dx / distance) * force, (dy / distance) * force),
                        body.worldCenter,
                        true
                    )*/
                }
            }
        }
    }

    private fun cleanup() {
        simulationJob?.cancel()
        particles.forEach { particle ->
            world?.destroyBody(particle.body)
        }
        particles.clear()
        bodies.clear()
        world = null
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
        val worldWidth = 60f
        val worldHeight = 40f

        val scaleX = size.width / worldWidth
        val scaleY = size.height / worldHeight
        val scale = min(scaleX, scaleY)

        val offsetX = (size.width - worldWidth * scale) / 2f
        val offsetY = size.height - (worldHeight * scale) / 2f

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
                radius = 0.2f * scale,
                center = screenPos,
                alpha = 0.8f
            )
        }

        // Draw bodies
        state.bodies.forEach { body ->
            when (body.type) {
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
    val gridSize = 5f

    for (x in -30..30 step gridSize.toInt()) {
        val screenX = x * scale + offsetX + (worldWidth * scale) / 2f
        drawLine(
            color = gridColor,
            start = Offset(screenX, 0f),
            end = Offset(screenX, size.height),
            strokeWidth = 1f
        )
    }

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

class PhysicsViewModelFactory(
    private val beverage: Beverage,
    private val onDrink: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhysicsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhysicsViewModel(
                beverageType = beverage.drinkType,
                beverageColor = Color(beverage.color),
                cbF = onDrink
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsSimulationScreen(
    beverage: Beverage,
    onDrink: () -> Unit,
) {
    val viewModel: PhysicsViewModel = viewModel(
        factory = PhysicsViewModelFactory(beverage, onDrink)
    )
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val gravitySensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    DisposableEffect(sensorManager, gravitySensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    viewModel.onSensorDataChanged(it)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do nothing
            }
        }

        // (Don't forget to add a null check for gravitySensor!)
        if (gravitySensor != null) {
            sensorManager.registerListener(
                listener,
                gravitySensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        // onDispose is called when the composable leaves the screen
        onDispose {
            // (Don't forget to add a null check for gravitySensor!)
            if (gravitySensor != null) {
                sensorManager.unregisterListener(listener)
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                viewModel.initSimulation(SimulationType.DRINK_TEMP)
                                      },
                            modifier = Modifier.fillMaxWidth(0.33f)
                        ) {
                            Text("Pour", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x00000000)
                ),
            )
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
                    val worldX = (offset.x / 20f) - 15f
                    val worldY = 20f - (offset.y / 20f)
                    viewModel.addWaterDrop(worldX, worldY)
                }
            )

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