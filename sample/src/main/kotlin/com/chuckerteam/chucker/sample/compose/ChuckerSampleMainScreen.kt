
package com.chuckerteam.chucker.sample.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chuckerteam.chucker.sample.InterceptorType
import com.chuckerteam.chucker.sample.R
import com.chuckerteam.chucker.sample.compose.testtags.ChuckerTestTags
import com.chuckerteam.chucker.sample.compose.theme.ChuckerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChuckerSampleMainScreen(
    widthSizeClass: WindowWidthSizeClass,
    selectedInterceptorType: InterceptorType,
    onInterceptorTypeChange: (InterceptorType) -> Unit,
    onInterceptorTypeLabelClick: () -> Unit,
    onDoHttp: () -> Unit,
    onDoGrpc: () -> Unit, // Added for gRPC
    onDoGraphQL: () -> Unit,
    onLaunchChucker: () -> Unit,
    onExportToLogFile: () -> Unit,
    onExportToHarFile: () -> Unit,
    isChuckerInOpMode: Boolean,
) {
    val isExpandedWidth = widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedWidth) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.intro_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.intro_body),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ChuckerSampleControls(
                            selectedInterceptorType = selectedInterceptorType,
                            onInterceptorTypeChange = onInterceptorTypeChange,
                            onInterceptorTypeLabelClick = onInterceptorTypeLabelClick,
                            onDoHttp = onDoHttp,
                            onDoGrpc = onDoGrpc, // Pass down
                            onDoGraphQL = onDoGraphQL,
                            onLaunchChucker = onLaunchChucker,
                            onExportToLogFile = onExportToLogFile,
                            onExportToHarFile = onExportToHarFile,
                            isChuckerInOpMode = isChuckerInOpMode,
                            isExpandedWidth = true,
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = { ChuckerSampleTopBar() },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.intro_body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                            .testTag(ChuckerTestTags.INTRO_BODY_TEXT_COMPACT),
                    )
                    ChuckerSampleControls(
                        selectedInterceptorType = selectedInterceptorType,
                        onInterceptorTypeChange = onInterceptorTypeChange,
                        onInterceptorTypeLabelClick = onInterceptorTypeLabelClick,
                        onDoHttp = onDoHttp,
                        onDoGrpc = onDoGrpc, // Pass down
                        onDoGraphQL = onDoGraphQL,
                        onLaunchChucker = onLaunchChucker,
                        onExportToLogFile = onExportToLogFile,
                        onExportToHarFile = onExportToHarFile,
                        isChuckerInOpMode = isChuckerInOpMode,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChuckerSampleMainScreenPreview() {
    ChuckerTheme {
        ChuckerSampleMainScreen(
            widthSizeClass = WindowWidthSizeClass.Compact,
            selectedInterceptorType = InterceptorType.APPLICATION,
            onInterceptorTypeChange = {},
            onInterceptorTypeLabelClick = {},
            onDoHttp = {},
            onDoGrpc = {},
            onDoGraphQL = {},
            onLaunchChucker = {},
            onExportToLogFile = {},
            onExportToHarFile = {},
            isChuckerInOpMode = true,
        )
    }
}
