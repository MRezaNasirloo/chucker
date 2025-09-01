
package com.chuckerteam.chucker.sample.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chuckerteam.chucker.sample.InterceptorType
import com.chuckerteam.chucker.sample.R
import com.chuckerteam.chucker.sample.compose.testtags.ChuckerTestTags
import com.chuckerteam.chucker.sample.compose.theme.AppAccentColor
import com.chuckerteam.chucker.sample.compose.theme.ChuckerTheme

@Composable
internal fun ChuckerSampleControls(
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
    isExpandedWidth: Boolean = false,
) {
    val modifier = if (isExpandedWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .widthIn(max = 500.dp)
            .fillMaxWidth()
    }
    val interceptorTypeLabel = stringResource(R.string.interceptor_type)
    Text(
        text = interceptorTypeLabel,
        style = MaterialTheme.typography.bodyLarge,
        color = AppAccentColor,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .testTag(ChuckerTestTags.CONTROLS_INTERCEPTOR_TYPE_LABEL)
            .clearAndSetSemantics { contentDescription = "$interceptorTypeLabel, opens external link, double tap to activate" }
            .clickable { onInterceptorTypeLabelClick.invoke() },
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        LabeledRadioButton(
            label = stringResource(R.string.application_type),
            selected = selectedInterceptorType == InterceptorType.APPLICATION,
            onClick = { onInterceptorTypeChange(InterceptorType.APPLICATION) },
            modifier = Modifier.weight(1f),
            index = 1,
        )
        LabeledRadioButton(
            label = stringResource(R.string.network_type),
            selected = selectedInterceptorType == InterceptorType.NETWORK,
            onClick = { onInterceptorTypeChange(InterceptorType.NETWORK) },
            modifier = Modifier.weight(1f),
            index = 2,
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    val buttonActions = listOf(
        stringResource(R.string.do_http_activity) to onDoHttp,
        stringResource(R.string.do_grpc_activity) to onDoGrpc, // Added gRPC action
        stringResource(R.string.do_graphql_activity) to onDoGraphQL,
    )

    buttonActions.forEach { (label, action) ->
        Button(
            onClick = action,
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(text = label)
        }
    }

    if (isChuckerInOpMode) {
        Button(
            onClick = onLaunchChucker,
            modifier = modifier.testTag(ChuckerTestTags.CONTROLS_LAUNCH_CHUCKER_BUTTON),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(text = stringResource(R.string.launch_chucker_directly))
        }
    }

    Spacer(modifier = Modifier.width(24.dp))
    if (isChuckerInOpMode) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onExportToLogFile,
                modifier = Modifier.weight(1f).testTag(ChuckerTestTags.CONTROLS_EXPORT_LOG_BUTTON),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(stringResource(R.string.export_to_file))
            }
            Button(
                onClick = onExportToHarFile,
                modifier = Modifier.weight(1f).testTag(ChuckerTestTags.CONTROLS_EXPORT_HAR_BUTTON),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(stringResource(R.string.export_to_file_har))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChuckerSampleControlsPreview() {
    ChuckerTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChuckerSampleControls(
                selectedInterceptorType = InterceptorType.NETWORK,
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
}
