package pk.shajranasab.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
@Composable fun ShajraTheme(dark:Boolean,content:@Composable()->Unit){
    val light=lightColorScheme(primary=Color(0xFF176B4D),secondary=Color(0xFFB08D32),background=Color(0xFFF7F4EA))
    val darkScheme=darkColorScheme(primary=Color(0xFF75D2AA),secondary=Color(0xFFD7BA63))
    MaterialTheme(colorScheme=if(dark)darkScheme else light,content=content)
}
