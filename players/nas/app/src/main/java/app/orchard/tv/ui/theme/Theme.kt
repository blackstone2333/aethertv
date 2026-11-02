package app.orchard.tv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Shared AetherTV geometry, palette and motion, copied into this independent Gradle root.
object Palette {
 val Background=Color(0xFF080B12)
 val Label=Color.White
 val SecondaryLabel=Color(0xFFB7BECF)
 val Glass=Color(0xCF20232D)
 val Fill=Color(0x28787880)
 val Focus=Color(0xFFF5F5F7)
 val OnFocus=Color(0xFF1D1D1F)
}
object Motion {
 val EaseOut=CubicBezierEasing(0.23f,1f,0.32f,1f)
 val EaseInOut=CubicBezierEasing(0.77f,0f,0.175f,1f)
 val Drawer=CubicBezierEasing(0.32f,0.72f,0f,1f)
}
val LocalReducedMotion=staticCompositionLocalOf { false }
object Type {
 val Body=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=23.sp,color=Palette.Label)
 val Title=Body.copy(fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.SemiBold)
 val Caption=Body.copy(fontSize=12.sp,lineHeight=17.sp,color=Palette.SecondaryLabel)
}
