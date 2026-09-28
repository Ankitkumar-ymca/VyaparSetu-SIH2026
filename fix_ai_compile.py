import re

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'r') as f:
    text = f.read()

# Fix Line 103:
pattern103 = r"messages = messages \+ ChatMessage\([\s]*text = \"I understood you want to do the following"
text = re.sub(pattern103, "addMessage(ChatMessage(\n                            text = \"I understood you want to do the following", text)

# Fix Line 174 items(messages) ... need to import items or use it properly? 
# Wait, items(messages) was passing because it's standard LazyColumn. 
# But let's make sure I also fix the ending bracket for addMessage
text = text.replace("                            action = proposedAction\n                        )", "                            action = proposedAction\n                        ))")

# Fix Line 216 & 217
pattern216 = r"updatedMessages\[messageIndex\] = message\.copy\(isExecuted = true\)[\s]*if \(result\.isSuccess\)"
replacement216 = "updatedMessages[index] = msgObj.copy(isExecuted = true)\n                                if (success)"
text = re.sub(pattern216, replacement216, text)

with open('app/src/main/java/com/example/presentation/ai/AiAssistantScreen.kt', 'w') as f:
    f.write(text)
