package com.example.w06

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.w06.ui.theme.GwaTheme
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private const val CHANNEL_ID = "bubble_game_channel"
private const val NOTIFICATION_ID = 1

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bubble Game Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

fun showNotification(context: Context, score: Int) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) return
    }

    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("게임 종료")
        .setContentText("최종 점수: $score 점!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwaTheme {
                BubbleGameScreen()
            }
        }
    }
}

@Composable
fun BubbleGameScreen() {
    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showNotification(context, lastScore)
        }

    var bubbles by remember { mutableStateOf(generateBubbles()) }
    var score by remember { mutableStateOf(0) }
    var timer by remember { mutableStateOf(30) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timer > 0) {
            delay(1000)
            timer--
        }
        showDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val hit = bubbles.find {
                        val dist = sqrt(
                            (it.x - offset.x).pow(2) + (it.y - offset.y).pow(2)
                        )
                        dist < it.radius
                    }
                    if (hit != null) {
                        score++
                        bubbles = bubbles - hit
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            bubbles.forEach {
                drawCircle(
                    color = Color(
                        Random.nextFloat(),
                        Random.nextFloat(),
                        Random.nextFloat()
                    ),
                    radius = it.radius,
                    center = Offset(it.x, it.y)
                )
            }
        }

        Text(
            "Score: $score",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        Text(
            "Time: $timer",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            fontWeight = FontWeight.Bold
        )
    }

    if (showDialog) {
        lastScore = score
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showNotification(context, score)
            }
        } else {
            showNotification(context, score)
        }

        AlertDialog(
            onDismissRequest = {},
            title = { Text("게임 종료") },
            text = { Text("최종 점수: $score") },
            confirmButton = {
                TextButton(onClick = {
                    bubbles = generateBubbles()
                    timer = 30
                    score = 0
                    showDialog = false
                }) {
                    Text("다시 시작")
                }
            }
        )
    }
}

data class Bubble(val x: Float, val y: Float, val radius: Float)

fun generateBubbles(): List<Bubble> {
    return List(15) {
        Bubble(
            x = Random.nextInt(100, 900).toFloat(),
            y = Random.nextInt(200, 1800).toFloat(),
            radius = Random.nextInt(40, 90).toFloat()
        )
    }
}

var lastScore = 0
