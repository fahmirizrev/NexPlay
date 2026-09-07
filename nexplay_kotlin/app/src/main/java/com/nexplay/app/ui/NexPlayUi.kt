package com.nexplay.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal object NexPlaySpacing {
    val screenPaddingX = 20.dp
    val headerPaddingTop = 8.dp
    val headerPaddingBottom = 6.dp
    val contentPaddingTop = 10.dp
    val contentPaddingBottom = 24.dp
    val sectionGap = 14.dp

    val rowVerticalPadding = 6.dp
    val listRowMinHeight = 52.dp
    val listLeadingSize = 40.dp
    val folderIconSize = 40.dp
    val listLeadingIconSize = 22.dp
    val listTextGap = 2.dp
    val itemGap = 10.dp

    val headerIconSize = 22.dp
    val headerIconButtonSize = 40.dp
    val headerActionGap = 4.dp
    val trailingIconSize = 22.dp
    val trailingIconBoxSize = 40.dp
    val rightEdgeIconOffsetX = 9.dp
}

@Composable
internal fun NexPlayAppHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    isPrimaryScreen: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = NexPlaySpacing.screenPaddingX,
                top = NexPlaySpacing.headerPaddingTop,
                end =
                    NexPlaySpacing.screenPaddingX -
                            NexPlaySpacing.rightEdgeIconOffsetX,
                bottom = NexPlaySpacing.headerPaddingBottom,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            NexPlayHeaderActionButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )

            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    if (isPrimaryScreen) {
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        )
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (actions != null) {
            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
internal fun NexPlayHeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(NexPlaySpacing.headerIconButtonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(
                NexPlaySpacing.headerIconSize,
            ),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
internal fun NexPlayLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun NexPlayEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = NexPlaySpacing.sectionGap,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (
            actionLabel != null &&
            onAction != null
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        8.dp,
                    ),
            )

            TextButton(
                onClick = onAction,
            ) {
                Text(
                    text = actionLabel,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                )
            }
        }
    }
}