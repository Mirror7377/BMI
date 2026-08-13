package com.example.bmi.ui.common

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bmi.R

@Composable
fun DiscardConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )

    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .align(Alignment.Center),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, top = 25.dp, end = 15.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.are_you_sure_you_want_to_delete_this_record),
                        fontSize = 13.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_regular)),
                        modifier = Modifier.align(Alignment.Start)
                    )


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF3659CF)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cancell),
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                            )
                        }

                        TextButton(
                            onClick = onConfirm,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF3659CF)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.montserrat_extrabold))
                            )
                        }
                    }
                }
            }
        }
    }
}