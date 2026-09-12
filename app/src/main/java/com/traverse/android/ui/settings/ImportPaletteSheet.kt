package com.traverse.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.traverse.android.ui.theme.palettePrimary

/**
 * 1:1 Kotlin port of the iOS `ImportPaletteView` (`Screens/SettingsView.swift`).
 *
 * The layout mirrors the SwiftUI `Form`: a descriptive footer, a "Palette Input" section with a
 * monospaced editor and an optional red error line, then an "Examples" section showing both a
 * coolors.co URL and an SCSS colour-variable block. The toolbar's Cancel / Import pair maps to
 * the sheet header, with Import disabled while the input is empty — exactly as on iOS.
 */
private const val COOLORS_EXAMPLE = "https://coolors.co/ff6ad5-c774e8-ad8cff-8795e8-94d0ff"

private val SCSS_EXAMPLE = listOf(
    "\$color1: #ff6ad5ff;",
    "\$color2: #c774e8ff;",
    "\$color3: #ad8cffff;",
    "\$color4: #8795e8ff;",
    "\$color5: #94d0ffff;"
).joinToString("\n")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPaletteSheet(
    paletteInput: String,
    onPaletteInputChange: (String) -> Unit,
    importError: String?,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // iOS toolbar: Cancel (cancellationAction) — "Import Palette" title — Import (confirmationAction)
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = onImport,
                        enabled = paletteInput.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(contentColor = palettePrimary)
                    ) {
                        Text("Import")
                    }
                }

                Text(
                    text = "Import Palette",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Section: description
            Text(
                text = "Import a custom color palette from coolors.co or paste SCSS color variables.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Section("Palette Input")
            SectionHeader("Palette Input")

            BasicTextField(
                value = paletteInput,
                onValueChange = onPaletteInputChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(12.dp)
            )

            if (importError != null) {
                Text(
                    text = importError,
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.Red),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Section("Examples")
            SectionHeader("Examples", modifier = Modifier.padding(top = 28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Coolors.co URL:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                SelectionContainer {
                    Text(
                        text = COOLORS_EXAMPLE,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                Text(
                    text = "SCSS Format:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                SelectionContainer {
                    Text(
                        text = SCSS_EXAMPLE,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}

/** iOS `Form` section header (small, secondary, uppercase-ish). */
@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.5f)
        ),
        modifier = modifier.padding(bottom = 8.dp)
    )
}
