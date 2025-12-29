package com.domity.cybertheme.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.domity.cybertheme.atoms.CyberSurface
import com.domity.cybertheme.foundation.CyberTheme

@Composable
fun CyberScaffold(
    modifier: Modifier = Modifier,
    useSafeArea: Boolean = true,
    topBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // 背景色容器
    CyberSurface(
        modifier = modifier.fillMaxSize(),
        color = CyberTheme.colors.background
    ) {
        // 布局骨架
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useSafeArea) Modifier.windowInsetsPadding(WindowInsets.systemBars)
                    else Modifier
                )
        ) {
            // 顶部栏区域
            if (topBar != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    topBar()
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }
    }
}