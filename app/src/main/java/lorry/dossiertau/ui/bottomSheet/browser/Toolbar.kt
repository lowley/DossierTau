package lorry.dossiertau.ui.bottomSheet.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import lorry.dossiertau.R
import lorry.dossiertau.TauApp
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserTarget

//implémentation ici: [[BrowserBottomToolbar]]

@Composable
fun BrowserToolbar(
    modifier: Modifier = Modifier,
    exitImageSelection: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(65.dp)
    ) {
        val browser: IBrowser by TauApp.instance.koin.inject()

//        Spacer(
//            modifier = Modifier.Companion
//                .padding(start = 50.dp, end = 50.dp, top = 5.dp, bottom = 0.dp)
//                .height(1.dp)
//                .fillMaxWidth()
//                .background(Color.Companion.LightGray)
//        )
        Row(
            Modifier.Companion
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(Color.Companion.Transparent)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            val state by browser.vm.state.collectAsState()

            Button(
                onClick = { state.goBack() },
                enabled = state.canGoBack,
                modifier = Modifier.Companion.padding(horizontal = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFe9c46a),
                    contentColor = Color.Companion.Black
                )
            )
            {
                Icon(
                    painter = painterResource(id = R.drawable.la_gauche),
                    contentDescription = "back",
                    modifier = Modifier.Companion.size(ButtonDefaults.IconSize),
                    tint = Color.Companion.Black
                )
            }

            Button(
                onClick = { browser.changeState(
                    item = null,
                    target = BrowserTarget.GOOGLE,
                ) },
                modifier = Modifier.Companion.padding(horizontal = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFe9c46a),
                    contentColor = Color.Companion.Black
                )
            )
            {
                Icon(
                    painter = painterResource(id = R.drawable.maison),
                    contentDescription = "home",
                    modifier = Modifier.Companion.size(ButtonDefaults.IconSize),
                    tint = Color.Companion.Black
                )
            }

            Button(
                onClick = exitImageSelection,
                modifier = Modifier.Companion.padding(horizontal = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFe9c46a),
                    contentColor = Color.Companion.Black
                )
            ) {
                Text(
                    "Retourner à l'application",
                    color = Color.Companion.Black
                )
            }

            Button(
                onClick = { state.goForward() },
                enabled = state.canGoForward,
                modifier = Modifier.Companion.padding(horizontal = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFe9c46a),
                    contentColor = Color.Companion.Black
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.la_droite),
                    contentDescription = "forward",
                    modifier = Modifier.Companion.size(ButtonDefaults.IconSize),
                    tint = Color.Companion.Black
                )
            }
        }
    }
}