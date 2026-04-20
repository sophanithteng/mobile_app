package com.example.assingment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.assingment.ui.theme.AssingmentTheme
import com.example.assingment.ui.theme.FoodPandaPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

// --- DATA MODELS ---
data class MealResponse(val meals: List<Meal>?)
data class Meal(val idMeal: String, val strMeal: String, val strMealThumb: String, var price: Double = 10.00)

// --- API ---
interface FoodApiService {
    @GET("filter.php?c=Seafood")
    suspend fun getSeafood(): MealResponse
}

object RetrofitClient {
    val api: FoodApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.themealdb.com/api/json/v1/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssingmentTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("dashboard") { DashboardScreen(navController) }
                    composable("menu") {
                        FoodMenuScreen(onMealClick = { meal ->
                            val encodedUrl = URLEncoder.encode(meal.strMealThumb, StandardCharsets.UTF_8.toString())
                            navController.navigate("details/${meal.strMeal}/$encodedUrl/${meal.price}")
                        }, onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "details/{name}/{url}/{price}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("url") { type = NavType.StringType },
                            navArgument("price") { type = NavType.FloatType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val url = backStackEntry.arguments?.getString("url") ?: ""
                        val price = backStackEntry.arguments?.getFloat("price")?.toDouble() ?: 10.0
                        FoodDetailScreen(name, url, price, 
                            onBack = { navController.popBackStack() },
                            onConfirm = { q, total -> 
                                val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                                navController.navigate("checkout/$encodedName/$q/$total")
                            }
                        )
                    }
                    composable(
                        route = "checkout/{name}/{quantity}/{totalPrice}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("quantity") { type = NavType.IntType },
                            navArgument("totalPrice") { type = NavType.FloatType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
                        val total = backStackEntry.arguments?.getFloat("totalPrice")?.toDouble() ?: 0.0
                        CheckoutScreen(name, quantity, total, 
                            onBack = { navController.popBackStack() },
                            onConfirm = { address, phone ->
                                val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                                val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString())
                                navController.navigate("confirmation/$encodedName/$quantity/$total/$encodedAddress/$phone")
                            }
                        )
                    }
                    composable(
                        route = "confirmation/{name}/{quantity}/{totalPrice}/{address}/{phone}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("quantity") { type = NavType.IntType },
                            navArgument("totalPrice") { type = NavType.FloatType },
                            navArgument("address") { type = NavType.StringType },
                            navArgument("phone") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
                        val total = backStackEntry.arguments?.getFloat("totalPrice")?.toDouble() ?: 0.0
                        val address = backStackEntry.arguments?.getString("address") ?: ""
                        val phone = backStackEntry.arguments?.getString("phone") ?: ""
                        OrderConfirmationScreen(name, quantity, total, address, phone, onHome = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        })
                    }
                }
            }
        }
    }
}

// --- LOGIN SCREEN (FoodPanda Style) ---
@Composable
fun LoginScreen(navController: androidx.navigation.NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            // FoodPanda Logo Placeholder (Pink circle with "fp")
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(FoodPandaPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("fp", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("foodpanda", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = FoodPandaPink)
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Login", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    )
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMessage = "Please enter both email and password"
                            } else {
                                scope.launch {
                                    isLoading = true
                                    delay(1500)
                                    isLoading = false
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Log In", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { navController.navigate("register") }, enabled = !isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Create an account", color = FoodPandaPink)
                    }
                }
            }
        }
    }
}

// --- REGISTER SCREEN (FoodPanda Style) ---
@Composable
fun RegisterScreen(navController: androidx.navigation.NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(FoodPandaPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("fp", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text("foodpanda", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FoodPandaPink)
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Create Account", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = null }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isLoading)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it; errorMessage = null }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isLoading)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it; errorMessage = null }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isLoading)
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                errorMessage = "All fields are required"
                            } else {
                                scope.launch {
                                    isLoading = true
                                    delay(1500)
                                    isLoading = false
                                    navController.navigate("login")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Sign Up", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { navController.navigate("login") }, enabled = !isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Already have an account? Log In", color = FoodPandaPink)
                    }
                }
            }
        }
    }
}

// --- DASHBOARD SCREEN (FoodPanda Style) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: androidx.navigation.NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("foodpanda", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FoodPandaPink),
                actions = {
                    IconButton(onClick = { navController.navigate("login") }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF7F7F7)).padding(16.dp)) {
            Text("Good morning!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Hungry? We've got you covered.", fontSize = 16.sp, color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            // FoodPanda Style Category Selectors (Horizontal)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCard("Food", Icons.Default.ShoppingCart, FoodPandaPink, Modifier.weight(1f))
                CategoryCard("Shops", Icons.Default.Info, Color(0xFF333333), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            Text("Your Statistics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Info Boxes (Reused logic but themed)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardCard("Total Orders", "12", Icons.Default.ShoppingCart, Color(0xFF17A2B8), Modifier.weight(1f))
                DashboardCard("Vouchers", "5", Icons.Default.Star, Color(0xFF28A745), Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = { navController.navigate("menu") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Order Seafood Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Column {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(title, fontSize = 14.sp, color = Color.White)
            }
            Icon(
                imageVector = icon, contentDescription = null,
                modifier = Modifier.size(48.dp).align(Alignment.CenterEnd).alpha(0.2f),
                tint = Color.White
            )
        }
    }
}

// --- MENU SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodMenuScreen(onMealClick: (Meal) -> Unit, onBack: () -> Unit) {
    var meals by remember { mutableStateOf<List<Meal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getSeafood()
            val fetchedMeals = response.meals ?: emptyList()
            fetchedMeals.forEachIndexed { index, meal -> meal.price = 9.99 + index }
            meals = fetchedMeals
            isLoading = false
        } catch (e: Exception) { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("foodpanda seafood", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FoodPandaPink)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF4F6F9))) {
                items(meals) { meal ->
                    FoodCard(meal, onClick = { onMealClick(meal) })
                }
            }
        }
    }
}

@Composable
fun FoodCard(meal: Meal, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).shadow(2.dp, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = meal.strMealThumb, contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.strMeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stringResource(R.string.price_format, meal.price), color = FoodPandaPink, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink)
            ) {
                Text("View")
            }
        }
    }
}

// --- DETAIL SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(name: String, imageUrl: String, pricePerItem: Double, onBack: () -> Unit, onConfirm: (Int, Double) -> Unit) {
    var quantity by remember { mutableStateOf(1) }
    val totalPrice = pricePerItem * quantity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FoodPandaPink)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6F9))) {
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize()) {
                AsyncImage(
                    model = imageUrl, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(4.dp)).shadow(4.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                Text(text = name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoStat(text = "4.5 Stars")
                    InfoStat(text = "250g")
                    InfoStat(text = "350 Cal")
                }
                Spacer(Modifier.height(24.dp))

                Text(text = "Ingredients", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                IngredientsRow()
                Spacer(Modifier.height(24.dp))

                Text(text = "Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(R.string.dummy_description), color = Color.Gray, lineHeight = 22.sp)
            }

            FloatingOrderBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                quantity = quantity,
                totalPrice = totalPrice,
                onAdd = { quantity++ },
                onRemove = { if (quantity > 1) quantity-- },
                onConfirmOrder = { onConfirm(quantity, totalPrice) }
            )
        }
    }
}

@Composable
fun InfoStat(text: String) {
    Text(text = text, color = Color.Gray, fontSize = 14.sp)
}

@Composable
fun IngredientsRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IngredientIcon(color = Color.Red, text = "Fish")
        IngredientIcon(color = Color.Green, text = "Herbs")
        IngredientIcon(color = Color.Yellow, text = "Spices")
    }
}

@Composable
fun IngredientIcon(color: Color, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(50.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(20.dp).background(color, CircleShape))
        }
        Text(text = text, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun FloatingOrderBar(modifier: Modifier = Modifier, quantity: Int, totalPrice: Double, onAdd: () -> Unit, onRemove: () -> Unit, onConfirmOrder: () -> Unit) {
    Surface(
        modifier = modifier.padding(16.dp).shadow(8.dp, RoundedCornerShape(4.dp)).fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(4.dp), color = Color.White
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEEEEE)).padding(4.dp)) {
                QuantityButton(text = "—", onClick = onRemove)
                Text(text = quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                QuantityButton(text = "+", onClick = onAdd)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onConfirmOrder,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Add to Cart - ${String.format(Locale.US, "$%.2f", totalPrice)}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuantityButton(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// --- CHECKOUT SCREEN (AdminLTE Style) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(name: String, quantity: Int, total: Double, onBack: () -> Unit, onConfirm: (String, String) -> Unit) {
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FoodPandaPink)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF4F6F9))
                .padding(16.dp)
        ) {
            Text("Delivery Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    OutlinedTextField(
                        value = address, onValueChange = { address = it; errorMessage = null },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        placeholder = { Text("Enter your full address") }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it; errorMessage = null },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        placeholder = { Text("e.g. +123456789") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Text("Order Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$name x$quantity", modifier = Modifier.weight(1f))
                    Text(String.format(Locale.US, "$%.2f", total), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (address.isEmpty() || phone.isEmpty()) {
                        errorMessage = "Please fill in all delivery details"
                    } else {
                        onConfirm(address, phone)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FoodPandaPink)
            ) {
                Text("Place Order", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// --- ORDER CONFIRMATION SCREEN (AdminLTE Card Style) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(name: String, quantity: Int, total: Double, address: String, phone: String, onHome: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Success", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF343A40)) // AdminLTE Dark
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF4F6F9))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AdminLTE Success Alert
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF28A745)) // AdminLTE Success
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Success! Your order has been placed.", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // AdminLTE Box (Card) for Order Details
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(0.dp)),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Box Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Text("Invoice #OR-7829", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Divider(color = Color(0xFFF4F4F4))

                    // Delivery Info
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ship To:", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(address, fontWeight = FontWeight.Medium)
                        Text("Phone: $phone", color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))

                        // Box Body (Table-like layout)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Product", fontWeight = FontWeight.Bold)
                            Text("Subtotal", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$name x$quantity")
                            Text(String.format(Locale.US, "$%.2f", total))
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    Text("Shipping: ", fontWeight = FontWeight.Bold)
                                    Text("$2.00")
                                }
                                Row {
                                    Text("Total: ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FoodPandaPink)
                                    Text(String.format(Locale.US, "$%.2f", total + 2.0), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FoodPandaPink)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)) // AdminLTE Primary
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}
