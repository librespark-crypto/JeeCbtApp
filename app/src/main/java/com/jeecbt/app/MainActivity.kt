package com.jeecbt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jeecbt.app.ui.HomeScreen
import com.jeecbt.app.ui.ResultScreen
import com.jeecbt.app.ui.TestScreen
import com.jeecbt.app.ui.theme.JeeCbtTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.init(this)

        setContent {
            JeeCbtTheme {
                val state by viewModel.state.collectAsState()

                when (state.phase) {
                    is AppPhase.Home -> HomeScreen(
                        viewModel = viewModel,
                        state     = state,
                        context   = this
                    )
                    is AppPhase.Test -> TestScreen(
                        viewModel = viewModel,
                        state     = state,
                        context   = this
                    )
                    is AppPhase.Result -> ResultScreen(
                        state   = state,
                        onRestart = { viewModel.restart(this) }
                    )
                }
            }
        }
    }
}
