package com.nexplay.app.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.min

internal class ChildLockPatternPreferenceStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PreferencesName,
                Context.MODE_PRIVATE,
            )

    fun hasPattern(): Boolean {
        return loadPattern() != null
    }

    fun save(
        pattern: List<Int>,
    ) {
        require(
            isValidChildLockPattern(
                pattern,
            ),
        )

        preferences
            .edit()
            .putString(
                PatternKey,
                encodeChildLockPattern(
                    pattern,
                ),
            )
            .remove(
                LegacyPinKey,
            )
            .apply()
    }

    fun matches(
        pattern: List<Int>,
    ): Boolean {
        return loadPattern() ==
                pattern
    }

    private fun loadPattern(): List<Int>? {
        return preferences
            .getString(
                PatternKey,
                null,
            )
            ?.let(
                ::decodeChildLockPattern,
            )
    }

    private companion object {
        const val PreferencesName =
            "nexplay_child_lock_preferences"

        const val PatternKey =
            "child_lock_pattern"

        const val LegacyPinKey =
            "child_lock_pin"
    }
}

internal fun isValidChildLockPattern(
    pattern: List<Int>,
): Boolean {
    return pattern.size >=
            MinimumPatternNodeCount &&
            pattern.distinct().size ==
            pattern.size &&
            pattern.all {
                it in 0 until
                        PatternNodeCount
            }
}

internal fun encodeChildLockPattern(
    pattern: List<Int>,
): String {
    require(
        isValidChildLockPattern(
            pattern,
        ),
    )

    return pattern.joinToString(
        separator = ",",
    )
}

internal fun decodeChildLockPattern(
    encodedPattern: String,
): List<Int>? {
    val parts =
        encodedPattern.split(',')

    val pattern =
        parts.map { part ->
            part.toIntOrNull()
                ?: return null
        }

    return pattern.takeIf(
        ::isValidChildLockPattern,
    )
}

@Composable
internal fun SetChildLockPatternDialog(
    onDismiss: () -> Unit,
    onPatternSaved:
        (List<Int>) -> Unit,
) {
    var firstPattern by
    remember {
        mutableStateOf<List<Int>?>(
            null,
        )
    }

    var errorMessage by
    remember {
        mutableStateOf<String?>(
            null,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    if (firstPattern == null) {
                        "Set Child Lock Pattern"
                    } else {
                        "Confirm Child Lock Pattern"
                    },
            )
        },
        text = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp,
                    ),
            ) {
                Text(
                    text =
                        if (firstPattern == null) {
                            "Draw a pattern using at least 4 dots."
                        } else {
                            "Draw the same pattern again to confirm."
                        },
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                ChildLockPatternInput(
                    modifier =
                        Modifier.size(
                            220.dp,
                        ),
                    onPatternComplete = {
                            pattern ->
                        when {
                            !isValidChildLockPattern(
                                pattern,
                            ) -> {
                                errorMessage =
                                    "Use at least 4 different dots."
                            }

                            firstPattern == null -> {
                                firstPattern =
                                    pattern
                                errorMessage =
                                    null
                            }

                            firstPattern ==
                                    pattern -> {
                                onPatternSaved(
                                    pattern,
                                )
                            }

                            else -> {
                                errorMessage =
                                    "Patterns do not match. Try again."
                            }
                        }
                    },
                )

                errorMessage
                    ?.let { message ->
                        Text(
                            text = message,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                        )
                    }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Cancel",
                )
            }
        },
    )
}

@Composable
internal fun VerifyChildLockPatternDialog(
    patternStore:
    ChildLockPatternPreferenceStore,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    var errorMessage by
    remember {
        mutableStateOf<String?>(
            null,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    "Unlock Child Lock",
            )
        },
        text = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp,
                    ),
            ) {
                Text(
                    text =
                        "Draw your Child Lock pattern.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                ChildLockPatternInput(
                    modifier =
                        Modifier.size(
                            220.dp,
                        ),
                    onPatternComplete = {
                            pattern ->
                        if (
                            patternStore.matches(
                                pattern,
                            )
                        ) {
                            onVerified()
                        } else {
                            errorMessage =
                                "Incorrect pattern."
                        }
                    },
                )

                errorMessage
                    ?.let { message ->
                        Text(
                            text = message,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                        )
                    }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Cancel",
                )
            }
        },
    )
}

@Composable
private fun ChildLockPatternInput(
    onPatternComplete:
        (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor =
        MaterialTheme
            .colorScheme
            .primary

    val inactiveColor =
        MaterialTheme
            .colorScheme
            .outline

    val selectedNodes =
        remember {
            mutableStateListOf<Int>()
        }

    var pointerPosition by
    remember {
        mutableStateOf<Offset?>(
            null,
        )
    }

    Canvas(
        modifier =
            modifier
                .aspectRatio(1f)
                .pointerInput(
                    Unit,
                ) {
                    awaitEachGesture {
                        val down =
                            awaitFirstDown()

                        selectedNodes.clear()
                        pointerPosition =
                            down.position

                        addPatternNodeAt(
                            position =
                                down.position,
                            size = size,
                            selectedNodes =
                                selectedNodes,
                        )

                        down.consume()

                        while (true) {
                            val event =
                                awaitPointerEvent()

                            val change =
                                event.changes
                                    .firstOrNull {
                                        it.id ==
                                                down.id
                                    }
                                    ?: break

                            pointerPosition =
                                change.position

                            addPatternNodeAt(
                                position =
                                    change.position,
                                size = size,
                                selectedNodes =
                                    selectedNodes,
                            )

                            if (!change.pressed) {
                                val completedPattern =
                                    selectedNodes.toList()

                                selectedNodes.clear()
                                pointerPosition =
                                    null

                                if (
                                    completedPattern
                                        .isNotEmpty()
                                ) {
                                    onPatternComplete(
                                        completedPattern,
                                    )
                                }

                                break
                            }

                            change.consume()
                        }
                    }
                },
    ) {
        val lineWidth =
            5.dp.toPx()

        selectedNodes
            .zipWithNext()
            .forEach {
                    (from, to) ->
                drawLine(
                    color = primaryColor,
                    start =
                        patternNodeCenter(
                            index = from,
                            width = size.width,
                            height = size.height,
                        ),
                    end =
                        patternNodeCenter(
                            index = to,
                            width = size.width,
                            height = size.height,
                        ),
                    strokeWidth =
                        lineWidth,
                    cap =
                        StrokeCap.Round,
                )
            }

        val lastNode =
            selectedNodes.lastOrNull()

        if (
            lastNode != null &&
            pointerPosition != null
        ) {
            drawLine(
                color =
                    primaryColor.copy(
                        alpha = 0.55f,
                    ),
                start =
                    patternNodeCenter(
                        index = lastNode,
                        width = size.width,
                        height = size.height,
                    ),
                end =
                    pointerPosition!!,
                strokeWidth =
                    lineWidth,
                cap =
                    StrokeCap.Round,
            )
        }

        repeat(
            PatternNodeCount,
        ) {
                index ->
            val selected =
                index in
                        selectedNodes

            drawCircle(
                color =
                    if (selected) {
                        primaryColor
                    } else {
                        inactiveColor
                    },
                radius =
                    if (selected) {
                        11.dp.toPx()
                    } else {
                        8.dp.toPx()
                    },
                center =
                    patternNodeCenter(
                        index = index,
                        width = size.width,
                        height = size.height,
                    ),
            )
        }
    }
}

private fun addPatternNodeAt(
    position: Offset,
    size: IntSize,
    selectedNodes:
    MutableList<Int>,
) {
    val node =
        findPatternNode(
            position = position,
            size = size,
        )
            ?: return

    if (node !in selectedNodes) {
        selectedNodes += node
    }
}

private fun findPatternNode(
    position: Offset,
    size: IntSize,
): Int? {
    val hitRadius =
        min(
            size.width,
            size.height,
        ) /
                6f *
                0.58f

    return (
            0 until
                    PatternNodeCount
            )
        .firstOrNull {
                index ->
            val center =
                patternNodeCenter(
                    index = index,
                    width =
                        size.width
                            .toFloat(),
                    height =
                        size.height
                            .toFloat(),
                )

            hypot(
                position.x -
                        center.x,
                position.y -
                        center.y,
            ) <= hitRadius
        }
}

private fun patternNodeCenter(
    index: Int,
    width: Float,
    height: Float,
): Offset {
    val column =
        index %
                PatternGridSize

    val row =
        index /
                PatternGridSize

    return Offset(
        x =
            width *
                    (
                            column * 2f +
                                    1f
                            ) /
                    (
                            PatternGridSize *
                                    2f
                            ),
        y =
            height *
                    (
                            row * 2f +
                                    1f
                            ) /
                    (
                            PatternGridSize *
                                    2f
                            ),
    )
}

private const val PatternGridSize = 3
private const val PatternNodeCount = 9
private const val MinimumPatternNodeCount = 4
