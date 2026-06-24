package com.example.linee_langer.ui.feature.camera.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.linee_langer.ui.feature.camera.model.BodyPart
import com.example.linee_langer.ui.theme.CameraOverlayText
import com.example.linee_langer.ui.theme.Dimens

@Composable
fun BodyPartCard(part: BodyPart, onClick: () -> Unit){
    Card(
        modifier = Modifier
            .size(Dimens.BodyPart)
            .clickable { onClick() },
        shape = RoundedCornerShape(Dimens.XLarge),
        colors = CardDefaults.cardColors(
            containerColor = CameraOverlayText.copy(alpha = 0.15f)
        ),
        border = BorderStroke(Dimens.BorderThin, CameraOverlayText.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(part.icon),
                contentDescription = "",
                tint = CameraOverlayText,
                modifier = Modifier.size(Dimens.CameraIconButton)
            )
            Spacer(modifier = Modifier.height(Dimens.Medium))
            Text(text = stringResource(part.name), color = CameraOverlayText, fontWeight = FontWeight.Medium)
        }
    }
}