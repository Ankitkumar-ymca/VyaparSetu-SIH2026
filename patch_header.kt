@Composable
fun HeaderSection(user: User?, business: Business?, onProfileClick: () -> Unit) {
    var greeting by remember { mutableStateOf(getGreetingMessage()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            greeting = getGreetingMessage()
            delay(60000L) // Check every minute
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = business?.name?.uppercase() ?: "VYAPARSETU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            val firstName = user?.name?.substringBefore(" ") ?: "User"
            Text(
                text = "$greeting, $firstName 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BlueLight, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .shadow(1.dp, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            val initials = user?.name?.split(" ")?.joinToString("") { it.take(1) }?.take(2)?.uppercase() ?: "US"
            Text(
                text = initials,
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

fun getGreetingMessage(): String {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (currentHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}
