package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VyaparSetu", appName)

    val whatIfTitle = context.getString(R.string.str_what_if_simulator)
    assertEquals("What-If Simulator", whatIfTitle)

    val disclaimer = context.getString(R.string.str_scenario_disclaimer)
    assertEquals("This is a hypothetical scenario. Your actual business data will not be changed.", disclaimer)

    val explainTitle = context.getString(R.string.str_explain_profit_change)
    assertEquals("Explain Profit Change", explainTitle)

    val whyDidProfitChange = context.getString(R.string.str_why_did_profit_change)
    assertEquals("Why did my profit change?", whyDidProfitChange)

    val whatsDrivingChange = context.getString(R.string.str_whats_driving_change)
    assertEquals("What's driving the change?", whatsDrivingChange)
  }
}
