package pk.shajranasab

import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.net.Uri
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import pk.shajranasab.data.FirebaseRepository
import pk.shajranasab.model.*
import pk.shajranasab.pdf.PdfExporter
import pk.shajranasab.ui.ShajraTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity: ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{App()}}
}
@Composable fun App(){
 val repo=remember{FirebaseRepository()};var dark by remember{mutableStateOf(false)};val nav=rememberNavController()
 ShajraTheme(dark){NavHost(nav,startDestination=if(repo.currentUid()==null)"login" else "tree"){
  composable("login"){Login(repo){nav.navigate("tree"){popUpTo("login"){inclusive=true}}}{nav.navigate("signup")}}
  composable("signup"){Signup(repo){nav.popBackStack()}}
  composable("tree"){Tree(repo,dark,{dark=!dark},{nav.navigate("person/new")},{id->nav.navigate("person/$id")},{nav.navigate("ann")},{nav.navigate("history")},{nav.navigate("login"){popUpTo(0)}})}
  composable("person/{id}"){b->PersonEditor(repo,b.arguments?.getString("id")){nav.popBackStack()}}
  composable("person/new"){PersonEditor(repo,null){nav.popBackStack()}}
  composable("ann"){Announcements(repo){nav.popBackStack()}}
  composable("history"){History(repo){nav.popBackStack()}}
 }}
}
@Composable fun Login(repo:FirebaseRepository,onOk:()->Unit,signup:()->Unit){
 val scope=rememberCoroutineScope();var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var msg by remember{mutableStateOf("")};var reset by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){
  Text("شجرہ نسب",style=MaterialTheme.typography.headlineLarge);Text("خاندان محمد علی",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(20.dp))
  OutlinedTextField(email,{email=it},label={Text("ای میل / Email")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
  OutlinedTextField(pass,{pass=it},label={Text("پاسورڈ / Password")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
  Button(modifier=Modifier.fillMaxWidth(),onClick={scope.launch{try{repo.signIn(email,pass);onOk()}catch(e:Exception){msg=e.message?:"Login failed"}}}){Text("لاگ ان")}
  TextButton({reset=true}){Text("پاسورڈ بھول گئے؟")};TextButton(signup){Text("نیا اکاؤنٹ بنائیں")};if(msg.isNotBlank())Text(msg)
 }
 if(reset)ResetDialog(repo,email,{reset=false})
}
@Composable fun ResetDialog(repo:FirebaseRepository,initialEmail:String,close:()->Unit){val scope=rememberCoroutineScope();var email by remember{mutableStateOf(initialEmail)};var pin by remember{mutableStateOf("")};var np by remember{mutableStateOf("")};var npp by remember{mutableStateOf("")};var newPin by remember{mutableStateOf("")};var msg by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("پاسورڈ ری سیٹ")},text={Column{OutlinedTextField(email,{email=it},label={Text("ای میل")});OutlinedTextField(pin,{pin=it},label={Text("PIN")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(np,{np=it},label={Text("نیا پاسورڈ")});OutlinedTextField(npp,{npp=it},label={Text("پاسورڈ دوبارہ")});OutlinedTextField(newPin,{newPin=it},label={Text("نیا PIN")});TextButton({scope.launch{try{repo.requestAdminReset(email);msg="PIN یاد نہیں تو ایڈمن سے مدد کی درخواست بھیج دی گئی۔"}catch(e:Exception){msg=e.message ?: "Error"}}}){Text("PIN بھول گئے؟ ایڈمن سے مدد لیں")};if(msg.isNotBlank())Text(msg)}},confirmButton={Button({scope.launch{try{if(np!=npp)error("پاسورڈ ایک جیسے نہیں");repo.resetByPin(email,pin,np,newPin);msg="پاسورڈ تبدیل ہوگیا۔"}catch(e:Exception){msg=e.message ?: "Reset failed"}}}){Text("محفوظ کریں")}},dismissButton={TextButton(close){Text("بند")}})}
@Composable fun Signup(repo:FirebaseRepository,onDone:()->Unit){val scope=rememberCoroutineScope();var name by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var pin by remember{mutableStateOf("")};var msg by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){Text("نیا اکاؤنٹ",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(name,{name=it},label={Text("نام")},modifier=Modifier.fillMaxWidth());OutlinedTextField(email,{email=it},label={Text("ای میل")},modifier=Modifier.fillMaxWidth());OutlinedTextField(pass,{pass=it},label={Text("پاسورڈ")},modifier=Modifier.fillMaxWidth());OutlinedTextField(pin,{pin=it},label={Text("4 یا 6 ہندسوں کا PIN")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());Text("یہ PIN محفوظ رکھیں — پاسورڈ بھولنے پر کام آئے گا");Button({scope.launch{try{repo.signUp(email,pass,name,pin);msg="اکاؤنٹ بن گیا۔ منظوری کا انتظار کریں۔";onDone()}catch(e:Exception){msg=e.message?:"Error"}}}){Text("سائن اپ")};Text(msg)}}
@Composable fun Tree(repo:FirebaseRepository,dark:Boolean,toggle:()->Unit,add:()->Unit,open:(String)->Unit,ann:()->Unit,history:()->Unit,logout:()->Unit){val scope=rememberCoroutineScope();var people by remember{mutableStateOf(emptyList<Person>())};var q by remember{mutableStateOf("")};var menu by remember{mutableStateOf(false)};var profile by remember{mutableStateOf<UserProfile?>(null)};LaunchedEffect(Unit){repo.peopleSnapshot{people=it};profile=repo.profile();try{repo.publishFcmToken()}catch(_:Exception){}}
 val filtered=people.filter{it.name.contains(q,true)||it.fatherName.contains(q,true)};Scaffold(topBar={TopAppBar(title={Text("شجرہ نسب خاندان محمد علی")},navigationIcon={IconButton({menu=!menu}){Icon(Icons.Default.Menu,"Menu")}},actions={Text("● آن لائن",color=MaterialTheme.colorScheme.primary);IconButton(add){Icon(Icons.Default.Add,"Add")}})},floatingActionButton={if(profile?.role=="admin"||profile?.role=="superAdmin")FloatingActionButton(add){Icon(Icons.Default.Add,"Add")}}){pad->Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)){OutlinedTextField(q,{q=it},label={Text("نام یا والد کا نام تلاش کریں")},singleLine=true,modifier=Modifier.fillMaxWidth());LazyColumn{items(filtered){p->ListItem(headlineContent={Text(p.name)},supportingContent={Text(if(p.fatherName.isBlank())"" else "ولد ${p.fatherName}")},modifier=Modifier.clickable{open(p.id)})}};if(menu)DropdownMenu(true,{menu=false}){DropdownMenuItem({Text("اعلانات")},{ann()});if(profile?.role=="admin"||profile?.role=="superAdmin")DropdownMenuItem({Text("ریکارڈ / ہسٹری")},{history()});DropdownMenuItem({Text(if(dark)"لائٹ تھیم" else "ڈارک تھیم")},{toggle()});DropdownMenuItem({Text("تعاون")},{scope.launch{}});DropdownMenuItem({Text("لاگ آؤٹ")},{logout()})}}}}
@Composable fun PersonEditor(repo:FirebaseRepository,id:String?,done:()->Unit){
 val scope=rememberCoroutineScope();val context=androidx.compose.ui.platform.LocalContext.current;var p by remember{mutableStateOf(Person(id=id?:""))};var people by remember{mutableStateOf(emptyList<Person>())};var loaded by remember{mutableStateOf(id==null)};var msg by remember{mutableStateOf("")};
 LaunchedEffect(id){if(id!=null)repo.person(id)?.let{p=it};repo.peopleOnce{people=it};loaded=true};if(!loaded)return
 Column(Modifier.fillMaxSize().padding(20.dp)){Text(if(id==null)"نیا فرد" else "فرد میں ترمیم",style=MaterialTheme.typography.headlineSmall);
 listOf("نام" to p.name,"والد کا نام" to p.fatherName,"والدہ کا نام" to p.motherName,"تاریخ پیدائش" to p.birthDate,"تاریخ وفات" to p.deathDate,"شریک حیات" to p.spouse,"Parent ID" to p.parentId,"نوٹس" to p.notes).forEach{(label,v)->OutlinedTextField(v,{x->p=when(label){"نام"->p.copy(name=x);"والد کا نام"->p.copy(fatherName=x);"والدہ کا نام"->p.copy(motherName=x);"تاریخ پیدائش"->p.copy(birthDate=x);"تاریخ وفات"->p.copy(deathDate=x);"شریک حیات"->p.copy(spouse=x);"Parent ID"->p.copy(parentId=x);else->p.copy(notes=x)}},label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=label!="نوٹس")};
 Button({scope.launch{try{repo.savePerson(p);done()}catch(e:Exception){msg=e.message?:"Error"}}}){Text("محفوظ کریں")};
 if(id!=null){Button({scope.launch{try{val path=PdfExporter.personLine(p,people);val f=PdfExporter.export(context,"شجرہ نسب خاندان محمد علی","شجرہ نسب برائے ${p.name}\n$path\nخاندانی شجرہ نسب (اولاد محمد علی)");val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",f);context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"PDF شیئر کریں"))}catch(e:Exception){msg=e.message?:"PDF error"}}}){Text("PDF بنائیں / شیئر کریں")};TextButton({scope.launch{repo.deletePerson(id);done()}}){Text("حذف کریں")}};Text(msg)}}
@Composable fun Announcements(repo:FirebaseRepository,back:()->Unit){val scope=rememberCoroutineScope();var list by remember{mutableStateOf(emptyList<Announcement>())};var role by remember{mutableStateOf("")};var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};LaunchedEffect(Unit){repo.announcements{list=it};role=repo.profile()?.role?:""};Column(Modifier.fillMaxSize().padding(16.dp)){Text("اعلانات / نوٹس بورڈ",style=MaterialTheme.typography.headlineSmall);if(role=="admin"||role=="superAdmin"){OutlinedTextField(title,{title=it},label={Text("عنوان")},modifier=Modifier.fillMaxWidth());OutlinedTextField(body,{body=it},label={Text("اعلان")},modifier=Modifier.fillMaxWidth());Button({scope.launch{repo.saveAnnouncement(Announcement(title=title,body=body));title="";body=""}}){Text("اعلان شائع کریں")}};LazyColumn{items(list){a->ListItem(headlineContent={Text(if(a.pinned)"📌 ${a.title}" else a.title)},supportingContent={Text("${a.body}\n${SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date(a.createdAt))}")})}};Button(back){Text("واپس")}}}
@Composable fun History(repo:FirebaseRepository,back:()->Unit){var list by remember{mutableStateOf(emptyList<AuditLog>())};var q by remember{mutableStateOf("")};LaunchedEffect(Unit){repo.auditSnapshot{list=it}};val f=list.filter{it.personName.contains(q,true)||it.action.contains(q,true)||it.actorName.contains(q,true)};Column(Modifier.fillMaxSize().padding(16.dp)){Text("ریکارڈ / ہسٹری",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(q,{q=it},label={Text("نام، تبدیلی یا صارف تلاش کریں")},modifier=Modifier.fillMaxWidth());LazyColumn{items(f){a->ListItem(headlineContent={Text("${a.action}: ${a.personName}")},supportingContent={Text("${a.actorName} — ${SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date(a.timestamp))}")})}};Button(back){Text("واپس")}}}
