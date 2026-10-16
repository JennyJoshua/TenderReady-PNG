package com.tenderready.png

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenderready.png.ui.AppNav
import com.tenderready.png.ui.MainViewModel
import com.tenderready.png.ui.TenderReadyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as TenderReadyApp).repository
        setContent {
            TenderReadyTheme {
                val vm: MainViewModel = viewModel(factory = SimpleFactory { MainViewModel(repo) })
                AppNav(vm)
            }
        }
    }
}

/** Minimal ViewModel factory so no extra library or DI framework is needed. */
class SimpleFactory(private val build: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = build() as T
}
