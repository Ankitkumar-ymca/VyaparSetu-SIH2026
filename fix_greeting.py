with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""var greeting by remember { mutableStateOf(getGreetingMessage()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            greeting = getGreetingMessage()
            delay(60000L) // Check every minute
        }
    }""",
"""var greetingId by remember { mutableIntStateOf(getGreetingMessageId()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            greetingId = getGreetingMessageId()
            delay(60000L) // Check every minute
        }
    }
    val greeting = stringResource(id = greetingId)"""
)

# And replace the function definition
content = content.replace(
"""@Composable
fun getGreetingMessage(): String {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (currentHour) {
        in 5..11 -> stringResource(R.string.str_good_morning)
        in 12..16 -> stringResource(R.string.str_good_afternoon)
        in 17..20 -> stringResource(R.string.str_good_evening)
        else -> stringResource(R.string.str_good_night)
    }
}""",
"""fun getGreetingMessageId(): Int {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (currentHour) {
        in 5..11 -> R.string.str_good_morning
        in 12..16 -> R.string.str_good_afternoon
        in 17..20 -> R.string.str_good_evening
        else -> R.string.str_good_night
    }
}"""
)

with open('app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

