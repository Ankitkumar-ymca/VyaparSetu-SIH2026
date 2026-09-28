with open('app/src/main/java/com/example/presentation/reports/GenericReportScreen.kt', 'r') as f:
    content = f.read()

if 'import com.example.R' not in content:
    content = content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.compose.ui.res.stringResource\nimport com.example.R')

content = content.replace('"$title Report"', 'stringResource(R.string.str_report_title_suffix, title)')

with open('app/src/main/java/com/example/presentation/reports/GenericReportScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/res/values/strings.xml', 'r') as f:
    xml = f.read()
if 'name="str_report_title_suffix"' not in xml:
    xml = xml.replace('</resources>', '    <string name="str_report_title_suffix">%1$s Report</string>\n</resources>')
    with open('app/src/main/res/values/strings.xml', 'w') as f:
        f.write(xml)

with open('app/src/main/res/values-hi/strings.xml', 'r') as f:
    hi_xml = f.read()
if 'name="str_report_title_suffix"' not in hi_xml:
    hi_xml = hi_xml.replace('</resources>', '    <string name="str_report_title_suffix">%1$s रिपोर्ट</string>\n</resources>')
    with open('app/src/main/res/values-hi/strings.xml', 'w') as f:
        f.write(hi_xml)

