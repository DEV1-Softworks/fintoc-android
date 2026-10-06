package mx.dev1.fintoc.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import mx.dev1.fintoc.sample.ui.theme.FintocSampleTheme

@Preview(showBackground = true)
@Composable
private fun SampleScreenPreview() {
    FintocSampleTheme {
        SampleScreen(isSdkInitialized = true)
    }
}
