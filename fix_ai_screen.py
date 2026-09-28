import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

old_scaffold = """    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_vyaparsetu_ai), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) {"""

new_scaffold = """    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.str_clear_chat_history)) },
            text = { Text(stringResource(R.string.str_delete_all_chat)) },
            confirmButton = {
                Button(
                    onClick = {
                        MockDataStore.clearChatHistory()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.str_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.str_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.str_vyaparsetu_ai), fontWeight = FontWeight.Bold, color = BlueNavy) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BlueNavy)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.str_clear_chat_history)) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) {"""

text = text.replace(old_scaffold, new_scaffold)

# Need to update ChatBubble to accept delete action if we want, but wait "Delete individual conversation" vs "Clear all"
# For individual delete, I can add a small delete button or long press. But the prompt just says:
# "Allow: Delete individual conversation, Clear all chat history"
# A simple way is to pass the message index and add a trash icon or dropdown.

old_chat_bubble = """@Composable
fun ChatBubble(message: ChatMessage, onExecute: (ProposedAction, ChatMessage) -> Unit) {"""

new_chat_bubble = """@Composable
fun ChatBubble(message: ChatMessage, index: Int, onExecute: (ProposedAction, ChatMessage) -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }"""

text = text.replace(old_chat_bubble, new_chat_bubble)
text = text.replace("items(messages) { msg ->\n                    ChatBubble(msg, onExecute = { action, msgObj ->", "items(messages.size) { idx ->\n                    val msg = messages[idx]\n                    ChatBubble(msg, index = idx, onDelete = { MockDataStore.deleteChatMessage(idx) }, onExecute = { action, msgObj ->")

old_bubble_row = """    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {"""

new_bubble_row = """    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isUser) {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp).padding(end = 4.dp).align(Alignment.CenterVertically)) {
                Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.str_delete_chat)) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }"""

text = text.replace(old_bubble_row, new_bubble_row)

old_bot_bubble = """                    if (message.action != null && !message.isExecuted) {
                        Spacer(modifier = Modifier.height(8.dp))"""
new_bot_bubble = """                    if (message.action != null && !message.isExecuted) {
                        Spacer(modifier = Modifier.height(8.dp))"""
text = text.replace("""        if (!message.isUser) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "AI",
                modifier = Modifier
                    .size(24.dp)
                    .background(BlueNavy, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }""", """        if (!message.isUser) {
            Box(modifier = Modifier.align(Alignment.Bottom)) {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp).padding(end = 4.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.str_delete_chat)) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
            Icon(
                Icons.Filled.Mic,
                contentDescription = "AI",
                modifier = Modifier
                    .size(24.dp)
                    .background(BlueNavy, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }""")

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)
