package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.presentation.auth.LoginScreen
import com.example.presentation.auth.RegisterScreen
import com.example.presentation.auth.SplashScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.inventory.ProductDetailScreen
import com.example.presentation.inventory.ProductFormScreen
import com.example.presentation.khata.CustomerDetailScreen
import com.example.presentation.khata.PartyFormScreen
import com.example.presentation.khata.SupplierDetailScreen
import com.example.presentation.onboarding.OnboardingScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    navController.navigate("splash") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate("onboarding") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("onboarding") {
            OnboardingScreen(
                onSetupComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("dashboard") {
            DashboardScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSubRoute = { route ->
                    navController.navigate(route)
                }
            )
        }
        
        
        composable("business_forecast") {
            com.example.presentation.ai.BusinessForecastScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }
        
        composable("ai_assistant") {

            com.example.presentation.ai.AiAssistantScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }

        // --- BUSINESS ALERTS ROUTE ---
        composable("business_alerts") {
            com.example.presentation.alerts.BusinessAlertsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetail = { productId -> navController.navigate("product_detail/$productId") }
            )
        }

        // --- BUSINESS ACTIONS ROUTE ---
        composable("business_actions") {
            com.example.presentation.actions.BusinessActionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAction = { action ->
                    when {
                        action.targetId != null && (action.category == com.example.domain.intelligence.BusinessActionCategory.RESTOCK ||
                                action.category == com.example.domain.intelligence.BusinessActionCategory.CLEAR_STOCK ||
                                action.category == com.example.domain.intelligence.BusinessActionCategory.REVIEW_PRICING) -> {
                            navController.navigate("product_detail/${action.targetId}")
                        }
                        action.targetId != null && (action.category == com.example.domain.intelligence.BusinessActionCategory.COLLECT_PAYMENT ||
                                action.category == com.example.domain.intelligence.BusinessActionCategory.REENGAGE_CUSTOMER) -> {
                            navController.navigate("customer_detail/${action.targetId}")
                        }
                        else -> {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        // --- BUSINESS WHAT-IF SCENARIO SIMULATOR ROUTE ---
        composable("business_scenario") {
            com.example.presentation.scenario.BusinessScenarioScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- CROSS-INTELLIGENCE BUSINESS INSIGHTS ROUTE ---
        composable("business_insights") {
            com.example.presentation.intelligence.BusinessInsightsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetail = { productId -> navController.navigate("product_detail/$productId") },
                onNavigateToCustomerDetail = { customerId -> navController.navigate("customer_detail/$customerId") },
                onNavigateToAlerts = { navController.navigate("business_alerts") },
                onNavigateToActions = { navController.navigate("business_actions") }
            )
        }

        // --- UNIFIED BUSINESS COMMAND CENTER ROUTE ---
        composable("command_center") {
            com.example.presentation.commandcenter.CommandCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProductDetail = { productId -> navController.navigate("product_detail/$productId") },
                onNavigateToCustomerDetail = { customerId -> navController.navigate("customer_detail/$customerId") },
                onNavigateToAlerts = { navController.navigate("business_alerts") },
                onNavigateToActions = { navController.navigate("business_actions") },
                onNavigateToInsights = { navController.navigate("business_insights") },
                onNavigateToProfitExplainability = { navController.navigate("profit_explainability") },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }
        
        // --- INVENTORY SUB ROUTES ---
        composable("product_form/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: "new"
            ProductFormScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("product_detail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("product_form/$id") }
            )
        }
        
        // --- KHATA SUB ROUTES ---
        composable("customer_form/{partyId}") { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: "new"
            PartyFormScreen(
                partyId = partyId,
                isCustomer = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // --- SALES SUB ROUTES ---
        composable("new_sale") {
            com.example.presentation.sales.NewSaleScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaleComplete = { saleId -> 
                    navController.popBackStack()
                    navController.navigate("sale_detail/$saleId")
                }
            )
        }
        
        composable("sale_detail/{saleId}") { backStackEntry ->
            val saleId = backStackEntry.arguments?.getString("saleId") ?: return@composable
            com.example.presentation.sales.SaleDetailScreen(
                saleId = saleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // --- PURCHASES SUB ROUTES ---
        composable("new_purchase") {
            com.example.presentation.purchases.NewPurchaseScreen(
                onNavigateBack = { navController.popBackStack() },
                onPurchaseComplete = { purchaseId -> 
                    navController.popBackStack()
                    navController.navigate("purchase_detail/$purchaseId")
                }
            )
        }
        
        composable("purchase_detail/{purchaseId}") { backStackEntry ->
            val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: return@composable
            com.example.presentation.purchases.PurchaseDetailScreen(
                purchaseId = purchaseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // --- EXPENSES SUB ROUTES ---
        composable("expenses") {
            com.example.presentation.expenses.ExpensesScreen(
                onNavigateToNewExpense = { navController.navigate("new_expense") },
                onEditExpense = { id -> navController.navigate("new_expense?expenseId=$id") }
            )
        }
        
        composable(
            route = "new_expense?expenseId={expenseId}",
            arguments = listOf(androidx.navigation.navArgument("expenseId") { 
                nullable = true; defaultValue = null 
            })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            com.example.presentation.expenses.NewExpenseScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // --- REPORTS SUB ROUTES ---
        composable("profit_loss") {
            com.example.presentation.reports.ProfitAndLossScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExplainability = { navController.navigate("profit_explainability") }
            )
        }

        composable("profit_explainability") {
            com.example.presentation.reports.ProfitExplainabilityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("cash_flow") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Cash Flow",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("sales_report") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Sales",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("purchases_report") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Purchases",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("inventory_report") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Inventory",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("customers_report") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Customers",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("suppliers_report") {
            com.example.presentation.reports.GenericReportScreen(
                title = "Suppliers",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("customer_detail/{partyId}") { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: return@composable
            CustomerDetailScreen(
                customerId = partyId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("customer_form/$id") }
            )
        }

        composable("supplier_form/{partyId}") { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: "new"
            PartyFormScreen(
                partyId = partyId,
                isCustomer = false,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("supplier_detail/{partyId}") { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: return@composable
            SupplierDetailScreen(
                supplierId = partyId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate("supplier_form/$id") }
            )
        }
    }
}
