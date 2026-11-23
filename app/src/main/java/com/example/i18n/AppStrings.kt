package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    val appTitle: String
    val tagline: String
    // Auth & Lock
    val loginTitle: String
    val username: String
    val password: String
    val rememberMe: String
    val signIn: String
    val registerAccount: String
    val createAccount: String
    val displayName: String
    val forgotPassword: String
    val forgotPasswordTitle: String
    val securityQuestion: String
    val securityAnswer: String
    val newPassword: String
    val resetPassword: String
    val restoreFromDriveLogin: String
    val defaultCredentialsHint: String
    val invalidCredentials: String
    val accountLockedError: String
    val userAlreadyExists: String
    val passwordUpdatedSuccess: String
    val wrongSecurityAnswer: String
    val adminPortalTitle: String
    val adminKeyLabel: String
    val enterAdminPortal: String
    val invalidAdminKey: String
    val lockScreenTitle: String
    val enterPinPrompt: String
    val wrongPinError: String
    val useBiometric: String
    val biometricPromptTitle: String
    val biometricPromptSubtitle: String
    val logout: String
    // Navigation
    val navDashboard: String
    val navLedger: String
    val navNotesTasks: String
    val navTools: String
    val navAnalytics: String
    val navWealth: String
    val navSettings: String
    // Dashboard
    val totalBalance: String
    val totalIncome: String
    val totalExpense: String
    val totalSavings: String
    val todaysIncome: String
    val todaysExpense: String
    val monthlyIncome: String
    val monthlyExpense: String
    val monthlySavings: String
    val currentBudgetStatus: String
    val recentTransactions: String
    val highestSpendingCategory: String
    val lowestSpendingCategory: String
    val monthlyOverview: String
    val interactiveCharts: String
    val quickActions: String
    val addIncome: String
    val addExpense: String
    val transfer: String
    val qrPayment: String
    val quickCalculator: String
    val calculatorConverterTitle: String
    val convertCurrencyTitle: String
    val useAsIncome: String
    val useAsExpense: String
    val savingsRateLabel: String
    val todayNetFlowLabel: String
    val upcomingBillsAndEmi: String
    val dueTodayBadge: String
    val overdueBadge: String
    val dueInDaysPrefix: String
    val daysSuffix: String
    val payOrPostNow: String
    val chart30DayCashflow: String
    val viewAll: String
    val noDataYet: String
    val startAddingTip: String
    // Transactions & Form
    val transactions: String
    val editTransaction: String
    val duplicate: String
    val favorite: String
    val unfavorite: String
    val delete: String
    val undo: String
    val transactionDeleted: String
    val restore: String
    val permanentDelete: String
    val trashBin: String
    val emptyTrash: String
    val emptyTrashDesc: String
    val calendarView: String
    val searchHint: String
    val filterAll: String
    val filterToday: String
    val filterYesterday: String
    val filterThisWeek: String
    val filterThisMonth: String
    val filterLastMonth: String
    val filterThisYear: String
    val filterCustomDate: String
    val income: String
    val expense: String
    val transferLabel: String
    val sortNewest: String
    val sortOldest: String
    val sortHighest: String
    val sortLowest: String
    val amount: String
    val category: String
    val account: String
    val targetAccount: String
    val paymentMethod: String
    val date: String
    val time: String
    val note: String
    val qrRefLabel: String
    val receiptImage: String
    val attachReceipt: String
    val removeReceipt: String
    val save: String
    val cancel: String
    val invalidAmountError: String
    val sameAccountTransferError: String
    val selectCategoryError: String
    val selectAccountError: String
    // Categories & Accounts & Budgets & Savings
    val categories: String
    val addCategory: String
    val editCategory: String
    val categoryName: String
    val customIcon: String
    val customColor: String
    val monthlyBudgetOptional: String
    val accounts: String
    val addAccount: String
    val editAccount: String
    val accountName: String
    val initialBalance: String
    val accountType: String
    val budgets: String
    val addBudget: String
    val budgetLimit: String
    val usedBudget: String
    val remainingBudget: String
    val budgetWarning80: String
    val budgetWarning90: String
    val budgetWarning100: String
    val savingsGoals: String
    val addGoal: String
    val goalName: String
    val targetAmount: String
    val currentSaved: String
    val remainingAmount: String
    val expectedDate: String
    val addContribution: String
    // Recurring & Extra Premium Modules
    val wealthHub: String
    val recurringTransactions: String
    val addRecurring: String
    val frequency: String
    val freqDaily: String
    val freqWeekly: String
    val freqMonthly: String
    val freqYearly: String
    val runDueNow: String
    val loansAndDebts: String
    val emiTracker: String
    val wishlist: String
    val investments: String
    val financialNotes: String
    val qrRecords: String
    val addRecord: String
    val counterparty: String
    val principalAmount: String
    val paidAmount: String
    val interestRate: String
    val monthlyEmi: String
    val assetName: String
    val investedAmount: String
    val currentValue: String
    val roiLabel: String
    val itemName: String
    val priority: String
    val noteTitle: String
    val noteBody: String
    val markPaid: String
    // Reports & Analytics
    val reports: String
    val analytics: String
    val dailyReport: String
    val weeklyReport: String
    val monthlyReport: String
    val yearlyReport: String
    val customReport: String
    val netBalance: String
    val growthRate: String
    val comparison: String
    val highestSpendingMonth: String
    val lowestSpendingMonth: String
    val highestIncomeMonth: String
    val highestExpenseCat: String
    val lowestExpenseCat: String
    val avgDailyExpense: String
    val avgMonthlyExpense: String
    val avgYearlyExpense: String
    val savingsTrend: String
    val financialHealthScore: String
    val smartSpendingInsights: String
    val expensePrediction: String
    val predictedNextMonth: String
    val pieChart: String
    val barChart: String
    val lineChart: String
    val areaChart: String
    val monthlyComparison: String
    val yearlyComparison: String
    val categoryComparison: String
    // Backup, Drive & Exports
    val backupAndRestore: String
    val googleDriveBackup: String
    val connectGmailDrive: String
    val connectedGmail: String
    val notConnected: String
    val backupNowToDrive: String
    val restoreFromDrive: String
    val autoBackupOnNewData: String
    val dailyAutoBackup: String
    val lastBackupLabel: String
    val neverBackedUp: String
    val driveBackupSuccess: String
    val driveRestoreSuccess: String
    val driveAuthNeeded: String
    val disconnectGmail: String
    val switchGmail: String
    val driveConsentCancelled: String
    val driveCheckingBackup: String
    val driveBackupFoundTitle: String
    val restoreNowAndLogin: String
    val syncPendingOffline: String
    val syncingNow: String
    val syncedJustNow: String
    val newPhoneRestoreHint: String
    val driveDisconnectedMsg: String
    val dualCloudVaultBadge: String
    val directDriveOAuthSync: String
    val saveToDriveAppBtn: String
    val restoreFromDriveAppBtn: String
    val oauth403GuideTitle: String
    val oauth403GuideBody: String
    val exportFormats: String
    val exportJson: String
    val exportCsv: String
    val exportExcel: String
    val exportPdf: String
    val restoreLocalJson: String
    val exportSuccess: String
    // Security & Settings & Notifications
    val settings: String
    val themeMode: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val currencyLabel: String
    val languageLabel: String
    val securityLock: String
    val pinLock: String
    val setPin: String
    val enter4DigitPin: String
    val biometricUnlock: String
    val biometricNotAvailable: String
    val notifications: String
    val budgetWarningsToggle: String
    val savingsRemindersToggle: String
    val monthlyReportReminderToggle: String
    val billRemindersToggle: String
    val markAllRead: String
    val changePassword: String
    val currentPassword: String
    val resetAllData: String
    val resetConfirmMsg: String
    val resetDetailsDesc: String
    val resetVerifyLabel: String
    val resetVerifyError: String
    val resetSuccessMsg: String
    val resetConfirmAction: String
    val pinLengthError: String
    val passwordEmptyError: String
    val notificationPermissionDenied: String
    val offlineFirstBadge: String
    // Admin Dashboard
    val adminDashboard: String
    val adminOverview: String
    val adminUsers: String
    val adminAllTransactions: String
    val adminSystemConfig: String
    val totalUsers: String
    val activeUsers: String
    val lockedUsers: String
    val systemVolume: String
    val addUser: String
    val lockUser: String
    val unlockUser: String
    val resetUserPassword: String
    val backupEntireDb: String
    val restoreEntireDb: String
    val exportAllData: String
    val clearAllTrash: String
    val databaseMaintenance: String
    val exitAdmin: String
    // Category defaults
    val catFood: String
    val catTransport: String
    val catShopping: String
    val catMedical: String
    val catEducation: String
    val catFamily: String
    val catBusiness: String
    val catSalary: String
    val catFreelancing: String
    val catInvestment: String
    val catSavings: String
    val catBills: String
    val catEntertainment: String
    val catTravel: String
    val catOthers: String
    // Multi-Profile System
    val switchProfile: String
    val activeProfileLabel: String
    val createProfile: String
    val editProfile: String
    val deleteProfile: String
    val profileName: String
    val profileDesc: String
    val separateFinanceHint: String
    val profileCreatedMsg: String
    // Keep Notes & Good Tasks Suite
    val notesAndTasks: String
    val keepNotesTab: String
    val goodTasksTab: String
    val addNote: String
    val editNote: String
    val noteTagsHint: String
    val checklistMode: String
    val addChecklistItem: String
    val wordCountLabel: String
    val charCountLabel: String
    val pinNote: String
    val archiveNote: String
    val unarchiveNote: String
    val activeNotesFilter: String
    val archivedFilter: String
    val trashFilter: String
    val addTask: String
    val editTask: String
    val taskTitleLabel: String
    val taskDescLabel: String
    val dueDateLabel: String
    val repeatLabel: String
    val subtasksLabel: String
    val addSubtaskHint: String
    val pendingTasksFilter: String
    val completedTasksFilter: String
    val highPriorityFilter: String
    // AI Assistant
    val aiAssistantTitle: String
    val chatWithAi: String
    val configureProviderFirst: String
    val manageProviders: String
    val addAiProvider: String
    val providerKindLabel: String
    val providerNameLabel: String
    val baseUrlLabel: String
    val apiKeyLabel: String
    val modelNameLabel: String
    val supportsVisionLabel: String
    val askAiHint: String
    val attachImage: String
    val clearChat: String
    val saveToNote: String
    val savedToNoteMsg: String
    val copiedMsg: String
    // Smart Tools Hub (27 Tools in 5 Categories)
    val smartToolsTitle: String
    val catTextTools: String
    val catFileTools: String
    val catCalcTools: String
    val catImageTools: String
    val catDailyTools: String
    val copyText: String
    val pasteText: String
    val exportTxt: String
    val exportPdfBtn: String
    val pickFileBtn: String
    val pickImageBtn: String
    val generateBtn: String
    val calculateBtn: String
    val allProfilesNetWorthLabel: String
    val askAiCfoLabel: String
    val searchToolsHint: String
    // Dashboard Timeframes & Games Arena
    val timeframeLifetime: String
    val timeframeThisYear: String
    val timeframeThisMonth: String
    val timeframeToday: String
    val lifetimeSavedHint: String
    val gamesArenaTitle: String
    val gamesArenaSubtitle: String
    val playGames: String
}

object BanglaStrings : AppStrings {
    override val appTitle = "FinPlay Pro"
    override val tagline = "স্মার্ট ফাইন্যান্স, নোটস, এআই ও Play & Earn ডুয়াল ওয়ালেট হাব"
    override val loginTitle = "অ্যাকাউন্টে লগইন করুন"
    override val username = "ইউজারনেম"
    override val password = "পাসওয়ার্ড"
    override val rememberMe = "লগইন মনে রাখুন"
    override val signIn = "লগইন"
    override val registerAccount = "নতুন অ্যাকাউন্ট তৈরি করুন"
    override val createAccount = "অ্যাকাউন্ট তৈরি"
    override val displayName = "পূর্ণ নাম"
    override val forgotPassword = "পাসওয়ার্ড ভুলে গেছেন?"
    override val forgotPasswordTitle = "পাসওয়ার্ড রিসেট করুন"
    override val securityQuestion = "নিরাপত্তা প্রশ্ন"
    override val securityAnswer = "নিরাপত্তা উত্তর"
    override val newPassword = "নতুন পাসওয়ার্ড"
    override val resetPassword = "পাসওয়ার্ড পরিবর্তন করুন"
    override val restoreFromDriveLogin = "গুগল ড্রাইভ থেকে ডাটা পুনরুদ্ধার"
    override val defaultCredentialsHint = "ডিফল্ট ইউজার: user | পাসওয়ার্ড: 1"
    override val invalidCredentials = "ইউজারনেম বা পাসওয়ার্ড সঠিক নয়।"
    override val accountLockedError = "এই অ্যাকাউন্টটি অ্যাডমিন কর্তৃক লক করা হয়েছে।"
    override val userAlreadyExists = "এই ইউজারনেমটি ইতিমধ্যেই ব্যবহৃত হচ্ছে।"
    override val passwordUpdatedSuccess = "পাসওয়ার্ড সফলভাবে পরিবর্তন হয়েছে।"
    override val wrongSecurityAnswer = "নিরাপত্তা উত্তর মেলেনি।"
    override val adminPortalTitle = "অ্যাডমিন পোর্টাল"
    override val adminKeyLabel = "অ্যাডমিন কী লিখুন"
    override val enterAdminPortal = "অ্যাডমিন ড্যাশবোর্ডে প্রবেশ"
    override val invalidAdminKey = "অ্যাডমিন কী সঠিক নয়।"
    override val lockScreenTitle = "অ্যাপ লক করা আছে"
    override val enterPinPrompt = "আপনার ৪-৬ সংখ্যার পিন দিন"
    override val wrongPinError = "পিন সঠিক নয়, আবার চেষ্টা করুন।"
    override val useBiometric = "ফিঙ্গারপ্রিন্ট / ফেস আনলক"
    override val biometricPromptTitle = "বায়োমেট্রিক যাচাইকরণ"
    override val biometricPromptSubtitle = "স্মার্ট ফাইন্যান্স লাইফ আনলক করতে টাচ করুন"
    override val logout = "লগআউট"
    override val navDashboard = "ড্যাশবোর্ড"
    override val navLedger = "হিসাব"
    override val navNotesTasks = "নোটস ও টাস্ক"
    override val navTools = "টুলস"
    override val navAnalytics = "বিশ্লেষণ"
    override val navWealth = "সম্পদ"
    override val navSettings = "সেটিংস"
    override val totalBalance = "মোট ব্যালেন্স"
    override val totalIncome = "মোট আয়"
    override val totalExpense = "মোট ব্যয়"
    override val totalSavings = "মোট সঞ্চয়"
    override val todaysIncome = "আজকের আয়"
    override val todaysExpense = "আজকের ব্যয়"
    override val monthlyIncome = "মাসিক আয়"
    override val monthlyExpense = "মাসিক ব্যয়"
    override val monthlySavings = "মাসিক সঞ্চয়"
    override val currentBudgetStatus = "চলতি বাজেটের অবস্থা"
    override val recentTransactions = "সাম্প্রতিক লেনদেন"
    override val highestSpendingCategory = "সর্বোচ্চ ব্যয়ের খাত"
    override val lowestSpendingCategory = "সর্বনিম্ন ব্যয়ের খাত"
    override val monthlyOverview = "মাসিক সারসংক্ষেপ"
    override val interactiveCharts = "ইন্টারঅ্যাক্টিভ চার্ট"
    override val quickActions = "দ্রুত কার্যক্রম"
    override val addIncome = "আয় যোগ"
    override val addExpense = "ব্যয় যোগ"
    override val transfer = "ট্রান্সফার"
    override val qrPayment = "QR পেমেন্ট"
    override val quickCalculator = "ক্যালকুলেটর"
    override val calculatorConverterTitle = "স্মার্ট ক্যালকুলেটর ও মুদ্রা রূপান্তর"
    override val convertCurrencyTitle = "মুদ্রা রূপান্তর (লাইভ)"
    override val useAsIncome = "+ আয় হিসেবে যোগ"
    override val useAsExpense = "- ব্যয় হিসেবে যোগ"
    override val savingsRateLabel = "সঞ্চয় হার"
    override val todayNetFlowLabel = "আজকের নিট প্রবাহ"
    override val upcomingBillsAndEmi = "আসন্ন বিল, কিস্তি (EMI) ও লক্ষ্যসমূহ"
    override val dueTodayBadge = "আজ বকেয়া"
    override val overdueBadge = "মেয়াদোত্তীর্ণ"
    override val dueInDaysPrefix = "বাকি"
    override val daysSuffix = "দিন"
    override val payOrPostNow = "এখনই পোস্ট"
    override val chart30DayCashflow = "৩০ দিনের প্রবাহ"
    override val viewAll = "সব দেখুন"
    override val noDataYet = "এখনও কোনো তথ্য নেই"
    override val startAddingTip = "স্বয়ংক্রিয় হিসাব শুরু করতে + আয় বা ব্যয় যোগ করুন।"
    override val transactions = "সকল লেনদেন"
    override val editTransaction = "লেনদেন সম্পাদনা"
    override val duplicate = "কপি করুন"
    override val favorite = "পছন্দ"
    override val unfavorite = "পছন্দ সরান"
    override val delete = "মুছুন"
    override val undo = "ফিরিয়ে আনুন"
    override val transactionDeleted = "লেনদেন ট্র্যাশে পাঠানো হয়েছে"
    override val restore = "পুনরুদ্ধার"
    override val permanentDelete = "স্থায়ীভাবে মুছুন"
    override val trashBin = "ট্র্যাশ বিন"
    override val emptyTrash = "ট্র্যাশ বিন খালি"
    override val emptyTrashDesc = "মুছে ফেলা লেনদেন এখানে জমা থাকে এবং যেকোনো সময় পুনরুদ্ধার করা যায়।"
    override val calendarView = "ক্যালেন্ডার ভিউ"
    override val searchHint = "খাত, টাকা, তারিখ, মাধ্যম বা নোট দিয়ে খুঁজুন..."
    override val filterAll = "সব"
    override val filterToday = "আজ"
    override val filterYesterday = "গতকাল"
    override val filterThisWeek = "এই সপ্তাহ"
    override val filterThisMonth = "এই মাস"
    override val filterLastMonth = "গত মাস"
    override val filterThisYear = "এই বছর"
    override val filterCustomDate = "নির্দিষ্ট তারিখ"
    override val income = "আয়"
    override val expense = "ব্যয়"
    override val transferLabel = "ট্রান্সফার"
    override val sortNewest = "নতুন আগে"
    override val sortOldest = "পুরোনো আগে"
    override val sortHighest = "বেশি টাকা আগে"
    override val sortLowest = "কম টাকা আগে"
    override val amount = "পরিমাণ"
    override val category = "ক্যাটাগরি"
    override val account = "অ্যাকাউন্ট"
    override val targetAccount = "গন্তব্য অ্যাকাউন্ট"
    override val paymentMethod = "পেমেন্ট মাধ্যম"
    override val date = "তারিখ"
    override val time = "সময়"
    override val note = "নোট / বিবরণ"
    override val qrRefLabel = "QR রেফারেন্স / মার্চেন্ট"
    override val receiptImage = "রশিদ / ভাউচার ছবি"
    override val attachReceipt = "ছবি সংযুক্ত করুন"
    override val removeReceipt = "ছবি সরান"
    override val save = "সংরক্ষণ করুন"
    override val cancel = "বাতিল"
    override val invalidAmountError = "শূন্যের চেয়ে বড় সঠিক পরিমাণ লিখুন।"
    override val sameAccountTransferError = "উৎস ও গন্তব্য অ্যাকাউন্ট আলাদা হতে হবে।"
    override val selectCategoryError = "একটি ক্যাটাগরি নির্বাচন করুন।"
    override val selectAccountError = "একটি অ্যাকাউন্ট নির্বাচন করুন।"
    override val categories = "ক্যাটাগরি"
    override val addCategory = "নতুন ক্যাটাগরি"
    override val editCategory = "ক্যাটাগরি সম্পাদনা"
    override val categoryName = "ক্যাটাগরির নাম"
    override val customIcon = "আইকন বাছুন"
    override val customColor = "রঙ বাছুন"
    override val monthlyBudgetOptional = "মাসিক বাজেট (ঐচ্ছিক)"
    override val accounts = "অ্যাকাউন্টসমূহ"
    override val addAccount = "নতুন অ্যাকাউন্ট"
    override val editAccount = "অ্যাকাউন্ট সম্পাদনা"
    override val accountName = "অ্যাকাউন্টের নাম"
    override val initialBalance = "প্রাথমিক ব্যালেন্স"
    override val accountType = "অ্যাকাউন্টের ধরন"
    override val budgets = "মাসিক বাজেট"
    override val addBudget = "বাজেট যোগ করুন"
    override val budgetLimit = "বাজেট সীমা"
    override val usedBudget = "ব্যবহৃত"
    override val remainingBudget = "অবশিষ্ট"
    override val budgetWarning80 = "সতর্কতা: আপনার বাজেটের ৮০% ব্যবহৃত হয়েছে!"
    override val budgetWarning90 = "জরুরি সতর্কতা: বাজেটের ৯০% শেষ!"
    override val budgetWarning100 = "বাজেট সীমা ১০০% পূর্ণ বা অতিক্রম করেছে!"
    override val savingsGoals = "সঞ্চয় লক্ষ্য"
    override val addGoal = "নতুন সঞ্চয় লক্ষ্য"
    override val goalName = "লক্ষ্যের নাম"
    override val targetAmount = "লক্ষ্যমাত্রা"
    override val currentSaved = "জমা হয়েছে"
    override val remainingAmount = "বাকি আছে"
    override val expectedDate = "সম্ভাব্য তারিখ"
    override val addContribution = "টাকা জমা করুন"
    override val wealthHub = "সম্পদ ও টুলস"
    override val recurringTransactions = "পুনরাবৃত্ত লেনদেন"
    override val addRecurring = "নতুন রিকারিং নিয়ম"
    override val frequency = "সময়কাল"
    override val freqDaily = "দৈনিক"
    override val freqWeekly = "সাপ্তাহিক"
    override val freqMonthly = "মাসিক"
    override val freqYearly = "বাৎসরিক"
    override val runDueNow = "বকেয়া এন্ট্রি এখনই চালান"
    override val loansAndDebts = "ঋণ ও দেনা ট্র্যাকার"
    override val emiTracker = "ইএমআই (EMI) ট্র্যাকার"
    override val wishlist = "উইশলিস্ট"
    override val investments = "বিনিয়োগ ট্র্যাকার"
    override val financialNotes = "আর্থিক নোটস"
    override val qrRecords = "QR পেমেন্ট রেকর্ড"
    override val addRecord = "নতুন এন্ট্রি যোগ"
    override val counterparty = "ব্যক্তি / প্রতিষ্ঠানের নাম"
    override val principalAmount = "মূল টাকা"
    override val paidAmount = "পরিশোধিত"
    override val interestRate = "সুদের হার (%)"
    override val monthlyEmi = "মাসিক কিস্তি"
    override val assetName = "সম্পদ / শেয়ারের নাম"
    override val investedAmount = "বিনিয়োগকৃত অর্থ"
    override val currentValue = "বর্তমান মূল্য"
    override val roiLabel = "লাভ / ক্ষতি (ROI)"
    override val itemName = "পণ্য / ইচ্ছার নাম"
    override val priority = "অগ্রাধিকার"
    override val noteTitle = "নোটের শিরোনাম"
    override val noteBody = "বিস্তারিত নোট"
    override val markPaid = "পরিশোধ যোগ"
    override val reports = "রিপোর্ট"
    override val analytics = "অ্যানালিটিক্স"
    override val dailyReport = "দৈনিক"
    override val weeklyReport = "সাপ্তাহিক"
    override val monthlyReport = "মাসিক"
    override val yearlyReport = "বাৎসরিক"
    override val customReport = "কাস্টম"
    override val netBalance = "নিট ব্যালেন্স"
    override val growthRate = "বৃদ্ধির হার"
    override val comparison = "তুলনামূলক চিত্র"
    override val highestSpendingMonth = "সর্বোচ্চ ব্যয়ের মাস"
    override val lowestSpendingMonth = "সর্বনিম্ন ব্যয়ের মাস"
    override val highestIncomeMonth = "সর্বোচ্চ আয়ের মাস"
    override val highestExpenseCat = "সর্বোচ্চ ব্যয়ের ক্যাটাগরি"
    override val lowestExpenseCat = "সর্বনিম্ন ব্যয়ের ক্যাটাগরি"
    override val avgDailyExpense = "গড় দৈনিক ব্যয়"
    override val avgMonthlyExpense = "গড় মাসিক ব্যয়"
    override val avgYearlyExpense = "গড় বাৎসরিক ব্যয়"
    override val savingsTrend = "সঞ্চয়ের প্রবণতা"
    override val financialHealthScore = "আর্থিক স্বাস্থ্য স্কোর"
    override val smartSpendingInsights = "স্মার্ট ব্যয় বিশ্লেষণ"
    override val expensePrediction = "ব্যয় পূর্বাভাস"
    override val predictedNextMonth = "আগামী মাসের সম্ভাব্য ব্যয়"
    override val pieChart = "পাই চার্ট"
    override val barChart = "বার চার্ট"
    override val lineChart = "লাইন চার্ট"
    override val areaChart = "এরিয়া চার্ট"
    override val monthlyComparison = "মাসিক তুলনা"
    override val yearlyComparison = "বাৎসরিক তুলনা"
    override val categoryComparison = "ক্যাটাগরি তুলনা"
    override val backupAndRestore = "গুগল ড্রাইভ ব্যাকআপ ও পুনরুদ্ধার"
    override val googleDriveBackup = "গুগল ড্রাইভ ক্লাউড ব্যাকআপ"
    override val connectGmailDrive = "জিমেইল / গুগল ড্রাইভ যুক্ত করুন"
    override val connectedGmail = "যুক্ত জিমেইল"
    override val notConnected = "যুক্ত নয়"
    override val backupNowToDrive = "এখনই ড্রাইভে ব্যাকআপ নিন"
    override val restoreFromDrive = "ড্রাইভ থেকে ডাটা ফিরিয়ে আনুন"
    override val autoBackupOnNewData = "নতুন ডাটা যোগ হলেই অটো ব্যাকআপ"
    override val dailyAutoBackup = "প্রতিদিন অটো ব্যাকআপ (দৈনিক)"
    override val lastBackupLabel = "সর্বশেষ ব্যাকআপ"
    override val neverBackedUp = "এখনও ব্যাকআপ নেওয়া হয়নি"
    override val driveBackupSuccess = "সকল হিসাব গুগল ড্রাইভে নিরাপদে সংরক্ষিত হয়েছে!"
    override val driveRestoreSuccess = "গুগল ড্রাইভ থেকে সকল হিসাব সফলভাবে পুনরুদ্ধার হয়েছে!"
    override val driveAuthNeeded = "প্রথমে আপনার জিমেইল অ্যাকাউন্ট যুক্ত করুন।"
    override val disconnectGmail = "জিমেইল সংযোগ বিচ্ছিন্ন করুন"
    override val switchGmail = "অন্য জিমেইল পরিবর্তন করুন"
    override val driveConsentCancelled = "গুগল ড্রাইভ অনুমোদন বাতিল হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
    override val driveCheckingBackup = "গুগল ড্রাইভে পূর্বের ব্যাকআপ খোঁজা হচ্ছে..."
    override val driveBackupFoundTitle = "গুগল ড্রাইভে ব্যাকআপ পাওয়া গেছে!"
    override val restoreNowAndLogin = "সব হিসাব রিস্টোর করে প্রবেশ করুন"
    override val syncPendingOffline = "অফলাইন পেন্ডিং — ইন্টারনেট পেলেই অটো ব্যাকআপ হবে"
    override val syncingNow = "ড্রাইভে সিঙ্ক হচ্ছে..."
    override val syncedJustNow = "ক্লাউড ভল্ট ও গুগল ড্রাইভে সিঙ্ক সম্পন্ন"
    override val newPhoneRestoreHint = "নতুন ফোনে আপনার যেকোনো জিমেইল লগইন করলেই সব হিসাব সাথে সাথে ফিরে আসবে"
    override val driveDisconnectedMsg = "জিমেইল অ্যাকাউন্ট বিচ্ছিন্ন করা হয়েছে।"
    override val dualCloudVaultBadge = "ডুয়াল ক্লাউড ভল্ট + গুগল ড্রাইভ সক্রিয় (Zero-403)"
    override val directDriveOAuthSync = "ডাইরেক্ট গুগল ড্রাইভ OAuth সিঙ্ক"
    override val saveToDriveAppBtn = "গুগল ড্রাইভ ফোল্ডারে ব্যাকআপ ফাইল সেভ করুন"
    override val restoreFromDriveAppBtn = "গুগল ড্রাইভ ফোল্ডার থেকে ব্যাকআপ রিস্টোর করুন"
    override val oauth403GuideTitle = "Error 403 সমাধান ও যেকোনো জিমেইল (md2005jim@gmail.com) গাইড"
    override val oauth403GuideBody = "১. ডুয়াল ক্লাউড ভল্ট (স্বয়ংক্রিয় সমাধান): এখন যেকোনো জিমেইল (যেমন md2005jim@gmail.com) যুক্ত করলেই Firebase Cloud Vault-এ সাথে সাথে অটো-ব্যাকআপ ও রিস্টোর হবে — কোনো Error 403 আসবে না!\n২. গুগল ড্রাইভ ফোল্ডার সেভ: উপরের 'গুগল ড্রাইভ ফোল্ডারে সেভ/রিস্টোর' বাটন চাপলে সরাসরি আপনার Google Drive অ্যাপে ফাইল সংরক্ষিত হবে।\n৩. Google Cloud Console স্থায়ী সেটিং: আপনি যদি ডাইরেক্ট OAuth স্ক্রিনও সবার জন্য চালু রাখতে চান, তবে Google Cloud Console -> APIs & Services -> OAuth consent screen-এ গিয়ে 'PUBLISH APP' বাটনে ১ বার ক্লিক করুন।"
    override val exportFormats = "ফাইল এক্সপোর্ট ও লোকাল রিস্টোর"
    override val exportJson = "JSON ব্যাকআপ"
    override val exportCsv = "CSV ফাইল"
    override val exportExcel = "Excel (.xls)"
    override val exportPdf = "PDF রিপোর্ট"
    override val restoreLocalJson = "JSON থেকে পুনরুদ্ধার"
    override val exportSuccess = "ফাইল সফলভাবে সংরক্ষিত হয়েছে"
    override val settings = "সেটিংস ও নিরাপত্তা"
    override val themeMode = "থিম মোড"
    override val themeLight = "লাইট"
    override val themeDark = "ডার্ক"
    override val themeSystem = "সিস্টেম"
    override val currencyLabel = "মুদ্রা নির্বাচন"
    override val languageLabel = "ভাষা (Language)"
    override val securityLock = "নিরাপত্তা ও অ্যাপ লক"
    override val pinLock = "পিন লক সক্রিয় করুন"
    override val setPin = "পিন সেট / পরিবর্তন"
    override val enter4DigitPin = "৪-৬ সংখ্যার পিন লিখুন"
    override val biometricUnlock = "ফিঙ্গারপ্রিন্ট / ফেস আনলক"
    override val biometricNotAvailable = "এই ডিভাইসে বায়োমেট্রিক সমর্থিত নয়"
    override val notifications = "নোটিফিকেশন কেন্দ্র"
    override val budgetWarningsToggle = "বাজেট সতর্কবার্তা (৮০%/৯০%/১০০%)"
    override val savingsRemindersToggle = "সঞ্চয় রিমাইন্ডার"
    override val monthlyReportReminderToggle = "মাসিক রিপোর্ট রিমাইন্ডার"
    override val billRemindersToggle = "বিল ও রিকারিং রিমাইন্ডার"
    override val markAllRead = "সব পঠিত চিহ্নিত করুন"
    override val changePassword = "পাসওয়ার্ড পরিবর্তন"
    override val currentPassword = "বর্তমান পাসওয়ার্ড"
    override val resetAllData = "সকল ডাটা রিসেট করুন"
    override val resetConfirmMsg = "আপনি কি নিশ্চিতভাবে এই অ্যাকাউন্টের সকল লেনদেন মুছে ফেলতে চান?"
    override val resetDetailsDesc = "সকল অ্যাকাউন্টের ব্যালেন্স, আয়, ব্যয়, লেনদেন, বাজেট, সঞ্চয় ও নোট ০ (শূন্য) হয়ে যাবে। আপনার অ্যাকাউন্ট ও ক্যাটাগরি ঠিক থাকবে এবং গুগল ড্রাইভ ব্যাকআপও আপডেট হবে।"
    override val resetVerifyLabel = "যাচাই করতে পাসওয়ার্ড বা পিন দিন"
    override val resetVerifyError = "পাসওয়ার্ড বা পিন সঠিক নয়।"
    override val resetSuccessMsg = "সকল ব্যালেন্স ও হিসাব সফলভাবে রিসেট হয়েছে।"
    override val resetConfirmAction = "সবকিছু রিসেট করুন"
    override val pinLengthError = "অন্তত ৪ থেকে ৬ সংখ্যার পিন লিখুন।"
    override val passwordEmptyError = "বর্তমান ও নতুন পাসওয়ার্ড উভয়ই লিখুন।"
    override val notificationPermissionDenied = "নোটিফিকেশন চালু করতে সিস্টেম অনুমতি প্রয়োজন।"
    override val offlineFirstBadge = "অফলাইন-ফার্স্ট ও অটো ক্লাউড ব্যাকআপ"
    override val adminDashboard = "অ্যাডমিন কন্ট্রোল প্যানেল"
    override val adminOverview = "সার্বিক চিত্র"
    override val adminUsers = "ইউজার ব্যবস্থাপনা"
    override val adminAllTransactions = "সকল লেনদেন"
    override val adminSystemConfig = "ডাটাবেস ও সিস্টেম"
    override val totalUsers = "মোট ইউজার"
    override val activeUsers = "সক্রিয় ইউজার"
    override val lockedUsers = "লক করা ইউজার"
    override val systemVolume = "সর্বমোট লেনদেন"
    override val addUser = "নতুন ইউজার যোগ"
    override val lockUser = "লক করুন"
    override val unlockUser = "আনলক করুন"
    override val resetUserPassword = "পাসওয়ার্ড রিসেট"
    override val backupEntireDb = "সম্পূর্ণ ডাটাবেস ব্যাকআপ"
    override val restoreEntireDb = "সম্পূর্ণ ডাটাবেস রিস্টোর"
    override val exportAllData = "সকল ডাটা এক্সপোর্ট"
    override val clearAllTrash = "সিস্টেম ট্র্যাশ পরিষ্কার"
    override val databaseMaintenance = "ডাটাবেস রক্ষণাবেক্ষণ"
    override val exitAdmin = "অ্যাডমিন প্যানেল থেকে বের হন"
    override val catFood = "খাবার"
    override val catTransport = "যাতায়াত"
    override val catShopping = "কেনাকাটা"
    override val catMedical = "চিকিৎসা"
    override val catEducation = "শিক্ষা"
    override val catFamily = "পরিবার"
    override val catBusiness = "ব্যবসা"
    override val catSalary = "বেতন"
    override val catFreelancing = "ফ্রিল্যান্সিং"
    override val catInvestment = "বিনিয়োগ"
    override val catSavings = "সঞ্চয়"
    override val catBills = "বিল"
    override val catEntertainment = "বিনোদন"
    override val catTravel = "ভ্রমণ"
    override val catOthers = "অন্যান্য"
    override val switchProfile = "প্রোফাইল বদল"
    override val activeProfileLabel = "সক্রিয় প্রোফাইল"
    override val createProfile = "নতুন প্রোফাইল"
    override val editProfile = "প্রোফাইল সম্পাদনা"
    override val deleteProfile = "প্রোফাইল মুছুন"
    override val profileName = "প্রোফাইলের নাম (যেমন: ফার্ম, পরিবার, ব্যবসা)"
    override val profileDesc = "বিবরণ (ঐচ্ছিক)"
    override val separateFinanceHint = "প্রতিটি প্রোফাইলের আয়, ব্যয়, অ্যাকাউন্ট ও বাজেট সম্পূর্ণ আলাদা।"
    override val profileCreatedMsg = "নতুন ফাইন্যান্স প্রোফাইল তৈরি হয়েছে"
    override val notesAndTasks = "কিপ নোটস ও গুড টাস্কস"
    override val keepNotesTab = "কিপ নোটস"
    override val goodTasksTab = "টাস্ক ম্যানেজার"
    override val addNote = "নতুন নোট"
    override val editNote = "নোট সম্পাদনা"
    override val noteTagsHint = "ট্যাগসমূহ (কমা দিয়ে)"
    override val checklistMode = "চেকলিস্ট মোড"
    override val addChecklistItem = "+ আইটেম যোগ করুন"
    override val wordCountLabel = "শব্দ"
    override val charCountLabel = "অক্ষর"
    override val pinNote = "পিন"
    override val archiveNote = "আর্কাইভ"
    override val unarchiveNote = "আন-আর্কাইভ"
    override val activeNotesFilter = "সক্রিয়"
    override val archivedFilter = "আর্কাইভ"
    override val trashFilter = "ট্র্যাশ"
    override val addTask = "নতুন টাস্ক"
    override val editTask = "টাস্ক সম্পাদনা"
    override val taskTitleLabel = "টাস্কের শিরোনাম"
    override val taskDescLabel = "বিস্তারিত"
    override val dueDateLabel = "শেষ সময়"
    override val repeatLabel = "পুনরাবৃত্তি"
    override val subtasksLabel = "সাবটাস্ক"
    override val addSubtaskHint = "সাবটাস্ক লিখুন"
    override val pendingTasksFilter = "বাকি"
    override val completedTasksFilter = "সম্পন্ন"
    override val highPriorityFilter = "জরুরি"
    override val aiAssistantTitle = "এআই অ্যাসিস্ট্যান্ট"
    override val chatWithAi = "Chat with AI"
    override val configureProviderFirst = "চ্যাট শুরু করতে প্রথমে Gemini, xKiro বা আপনার কাস্টম AI প্রোভাইডার API Key যুক্ত করুন।"
    override val manageProviders = "AI প্রোভাইডার"
    override val addAiProvider = "নতুন প্রোভাইডার যুক্ত করুন"
    override val providerKindLabel = "প্রোভাইডারের ধরন"
    override val providerNameLabel = "প্রোভাইডারের নাম"
    override val baseUrlLabel = "Base URL"
    override val apiKeyLabel = "API Key"
    override val modelNameLabel = "মডেল আইডি (Model ID)"
    override val supportsVisionLabel = "ইমেজ / ভিশন সাপোর্ট করে"
    override val askAiHint = "এআই-কে যেকোনো প্রশ্ন করুন..."
    override val attachImage = "ছবি"
    override val clearChat = "চ্যাট পরিষ্কার"
    override val saveToNote = "নোটে সেভ"
    override val savedToNoteMsg = "কিপ নোটে সংরক্ষণ করা হয়েছে"
    override val copiedMsg = "কপি হয়েছে"
    override val smartToolsTitle = "স্মার্ট টুলস হাব"
    override val catTextTools = "টেক্সট টুলস"
    override val catFileTools = "ফাইল কনভার্টার"
    override val catCalcTools = "ক্যালকুলেটর"
    override val catImageTools = "ইমেজ টুলস"
    override val catDailyTools = "দৈনন্দিন টুলস"
    override val copyText = "কপি"
    override val pasteText = "পেস্ট"
    override val exportTxt = ".TXT সেভ"
    override val exportPdfBtn = "PDF সেভ"
    override val pickFileBtn = "ফাইল বাছুন"
    override val pickImageBtn = "ছবি বাছুন"
    override val generateBtn = "তৈরি করুন"
    override val calculateBtn = "হিসাব করুন"
    override val allProfilesNetWorthLabel = "সব প্রোফাইলের নিট সম্পদ"
    override val askAiCfoLabel = "AI পরামর্শ"
    override val searchToolsHint = "৩৮+ স্মার্ট টুলস থেকে খুঁজুন..."
    override val timeframeLifetime = "আজীবন (Lifetime)"
    override val timeframeThisYear = "এই বছর"
    override val timeframeThisMonth = "এই মাস"
    override val timeframeToday = "আজ"
    override val lifetimeSavedHint = "সকল হিসাব স্থায়ীভাবে (Lifetime) সেভ থাকে — না মুছলে হারাবে না"
    override val gamesArenaTitle = "Play & Earn • গেম ও আর্নিং"
    override val gamesArenaSubtitle = "ডুয়াল ওয়ালেট • অনলাইন ফ্রেন্ডস • টুর্নামেন্ট ১০০% ক্যাশব্যাক ও আর্মরি"
    override val playGames = "খেলুন ও জিতুন"
}

object EnglishStrings : AppStrings {
    override val appTitle = "FinPlay Pro"
    override val tagline = "Multi-Profile Finance, Notes, AI & Play & Earn Dual-Wallet Hub"
    override val loginTitle = "Sign In to Your Ledger"
    override val username = "Username"
    override val password = "Password"
    override val rememberMe = "Remember session"
    override val signIn = "Sign In"
    override val registerAccount = "Create New Account"
    override val createAccount = "Create Account"
    override val displayName = "Full Name"
    override val forgotPassword = "Forgot Password?"
    override val forgotPasswordTitle = "Reset Password"
    override val securityQuestion = "Security Question"
    override val securityAnswer = "Security Answer"
    override val newPassword = "New Password"
    override val resetPassword = "Update Password"
    override val restoreFromDriveLogin = "Restore from Google Drive"
    override val defaultCredentialsHint = "Default User: user | Password: 1"
    override val invalidCredentials = "Invalid username or password."
    override val accountLockedError = "This account has been locked by the administrator."
    override val userAlreadyExists = "Username already exists."
    override val passwordUpdatedSuccess = "Password updated successfully."
    override val wrongSecurityAnswer = "Security answer does not match."
    override val adminPortalTitle = "Admin Portal"
    override val adminKeyLabel = "Enter Admin Key"
    override val enterAdminPortal = "Access Admin Dashboard"
    override val invalidAdminKey = "Invalid Admin Key."
    override val lockScreenTitle = "Vault Locked"
    override val enterPinPrompt = "Enter your 4–6 digit PIN"
    override val wrongPinError = "Incorrect PIN. Please try again."
    override val useBiometric = "Fingerprint / Face Unlock"
    override val biometricPromptTitle = "Biometric Verification"
    override val biometricPromptSubtitle = "Authenticate to unlock Smart Finance Life"
    override val logout = "Sign Out"
    override val navDashboard = "Home"
    override val navLedger = "Ledger"
    override val navNotesTasks = "Notes"
    override val navTools = "Tools"
    override val navAnalytics = "Analytics"
    override val navWealth = "Wealth"
    override val navSettings = "Settings"
    override val totalBalance = "Total Balance"
    override val totalIncome = "Total Income"
    override val totalExpense = "Total Expense"
    override val totalSavings = "Total Savings"
    override val todaysIncome = "Today's Income"
    override val todaysExpense = "Today's Expense"
    override val monthlyIncome = "Monthly Income"
    override val monthlyExpense = "Monthly Expense"
    override val monthlySavings = "Monthly Savings"
    override val currentBudgetStatus = "Current Budget Status"
    override val recentTransactions = "Recent Transactions"
    override val highestSpendingCategory = "Top Spending Category"
    override val lowestSpendingCategory = "Lowest Spending Category"
    override val monthlyOverview = "Monthly Overview"
    override val interactiveCharts = "Interactive Charts"
    override val quickActions = "Quick Actions"
    override val addIncome = "Income"
    override val addExpense = "Expense"
    override val transfer = "Transfer"
    override val qrPayment = "QR Pay"
    override val quickCalculator = "Calculator"
    override val calculatorConverterTitle = "Smart Calculator & Currency Converter"
    override val convertCurrencyTitle = "Live Currency Converter"
    override val useAsIncome = "+ Add as Income"
    override val useAsExpense = "- Add as Expense"
    override val savingsRateLabel = "Savings Rate"
    override val todayNetFlowLabel = "Today's Net Flow"
    override val upcomingBillsAndEmi = "Upcoming Bills, EMIs & Goals"
    override val dueTodayBadge = "Due Today"
    override val overdueBadge = "Overdue"
    override val dueInDaysPrefix = "In"
    override val daysSuffix = "d"
    override val payOrPostNow = "Pay / Post"
    override val chart30DayCashflow = "30-Day Cashflow"
    override val viewAll = "View All"
    override val noDataYet = "No records yet"
    override val startAddingTip = "Tap + Income or Expense to begin real-time tracking."
    override val transactions = "Transactions"
    override val editTransaction = "Edit Transaction"
    override val duplicate = "Duplicate"
    override val favorite = "Favorite"
    override val unfavorite = "Remove Favorite"
    override val delete = "Delete"
    override val undo = "Undo"
    override val transactionDeleted = "Transaction moved to Trash Bin"
    override val restore = "Restore"
    override val permanentDelete = "Delete Forever"
    override val trashBin = "Trash Bin"
    override val emptyTrash = "Trash Bin is Empty"
    override val emptyTrashDesc = "Deleted transactions stay here safely so you can restore them anytime."
    override val calendarView = "Calendar View"
    override val searchHint = "Search category, amount, date, account, method, note..."
    override val filterAll = "All"
    override val filterToday = "Today"
    override val filterYesterday = "Yesterday"
    override val filterThisWeek = "This Week"
    override val filterThisMonth = "This Month"
    override val filterLastMonth = "Last Month"
    override val filterThisYear = "This Year"
    override val filterCustomDate = "Custom Date"
    override val income = "Income"
    override val expense = "Expense"
    override val transferLabel = "Transfer"
    override val sortNewest = "Newest First"
    override val sortOldest = "Oldest First"
    override val sortHighest = "Highest Amount"
    override val sortLowest = "Lowest Amount"
    override val amount = "Amount"
    override val category = "Category"
    override val account = "Account"
    override val targetAccount = "Target Account"
    override val paymentMethod = "Payment Method"
    override val date = "Date"
    override val time = "Time"
    override val note = "Note"
    override val qrRefLabel = "QR Reference / Merchant"
    override val receiptImage = "Receipt Image"
    override val attachReceipt = "Attach Receipt"
    override val removeReceipt = "Remove"
    override val save = "Save"
    override val cancel = "Cancel"
    override val invalidAmountError = "Please enter an amount greater than 0."
    override val sameAccountTransferError = "Source and target accounts must differ."
    override val selectCategoryError = "Please select a category."
    override val selectAccountError = "Please select an account."
    override val categories = "Categories"
    override val addCategory = "New Category"
    override val editCategory = "Edit Category"
    override val categoryName = "Category Name"
    override val customIcon = "Choose Icon"
    override val customColor = "Choose Color"
    override val monthlyBudgetOptional = "Monthly Budget (Optional)"
    override val accounts = "Accounts"
    override val addAccount = "New Account"
    override val editAccount = "Edit Account"
    override val accountName = "Account Name"
    override val initialBalance = "Initial Balance"
    override val accountType = "Account Type"
    override val budgets = "Monthly Budgets"
    override val addBudget = "Add Budget"
    override val budgetLimit = "Budget Limit"
    override val usedBudget = "Used"
    override val remainingBudget = "Remaining"
    override val budgetWarning80 = "Alert: 80% of your monthly budget reached!"
    override val budgetWarning90 = "Warning: 90% of your monthly budget used!"
    override val budgetWarning100 = "Exceeded: 100% of monthly budget reached!"
    override val savingsGoals = "Savings Goals"
    override val addGoal = "New Savings Goal"
    override val goalName = "Goal Name"
    override val targetAmount = "Target Amount"
    override val currentSaved = "Current Saved"
    override val remainingAmount = "Remaining"
    override val expectedDate = "Expected Date"
    override val addContribution = "Add Deposit"
    override val wealthHub = "Wealth & Tools"
    override val recurringTransactions = "Recurring Rules"
    override val addRecurring = "New Recurring Rule"
    override val frequency = "Frequency"
    override val freqDaily = "Daily"
    override val freqWeekly = "Weekly"
    override val freqMonthly = "Monthly"
    override val freqYearly = "Yearly"
    override val runDueNow = "Process Due Entries"
    override val loansAndDebts = "Loans & Debts"
    override val emiTracker = "EMI Tracker"
    override val wishlist = "Wishlist"
    override val investments = "Investments"
    override val financialNotes = "Financial Notes"
    override val qrRecords = "QR Payment Log"
    override val addRecord = "Add Entry"
    override val counterparty = "Person / Institution"
    override val principalAmount = "Principal Amount"
    override val paidAmount = "Paid Amount"
    override val interestRate = "Interest Rate (%)"
    override val monthlyEmi = "Monthly EMI"
    override val assetName = "Asset / Holding Name"
    override val investedAmount = "Invested Capital"
    override val currentValue = "Current Value"
    override val roiLabel = "Return (ROI)"
    override val itemName = "Item Name"
    override val priority = "Priority"
    override val noteTitle = "Title"
    override val noteBody = "Notes"
    override val markPaid = "Record Payment"
    override val reports = "Reports"
    override val analytics = "Analytics"
    override val dailyReport = "Daily"
    override val weeklyReport = "Weekly"
    override val monthlyReport = "Monthly"
    override val yearlyReport = "Yearly"
    override val customReport = "Custom"
    override val netBalance = "Net Balance"
    override val growthRate = "Growth Rate"
    override val comparison = "Period Comparison"
    override val highestSpendingMonth = "Highest Spending Month"
    override val lowestSpendingMonth = "Lowest Spending Month"
    override val highestIncomeMonth = "Highest Income Month"
    override val highestExpenseCat = "Highest Expense Category"
    override val lowestExpenseCat = "Lowest Expense Category"
    override val avgDailyExpense = "Avg Daily Expense"
    override val avgMonthlyExpense = "Avg Monthly Expense"
    override val avgYearlyExpense = "Avg Yearly Expense"
    override val savingsTrend = "Savings Trend"
    override val financialHealthScore = "Financial Health Score"
    override val smartSpendingInsights = "Smart Spending Insights"
    override val expensePrediction = "Expense Prediction"
    override val predictedNextMonth = "Forecast Next Month"
    override val pieChart = "Pie Chart"
    override val barChart = "Bar Chart"
    override val lineChart = "Line Chart"
    override val areaChart = "Area Chart"
    override val monthlyComparison = "Monthly Comparison"
    override val yearlyComparison = "Yearly Comparison"
    override val categoryComparison = "Category Comparison"
    override val backupAndRestore = "Google Drive Backup & Recovery"
    override val googleDriveBackup = "Google Drive Cloud Backup"
    override val connectGmailDrive = "Connect Gmail / Google Drive"
    override val connectedGmail = "Connected Gmail"
    override val notConnected = "Not connected"
    override val backupNowToDrive = "Backup Now to Google Drive"
    override val restoreFromDrive = "Restore from Google Drive"
    override val autoBackupOnNewData = "Auto-backup when new data is added"
    override val dailyAutoBackup = "Daily scheduled auto-backup"
    override val lastBackupLabel = "Last Backup"
    override val neverBackedUp = "Never backed up"
    override val driveBackupSuccess = "All financial data backed up to Google Drive!"
    override val driveRestoreSuccess = "All financial data restored from Google Drive!"
    override val driveAuthNeeded = "Connect your Gmail account first to sync Drive."
    override val disconnectGmail = "Disconnect Gmail"
    override val switchGmail = "Switch Gmail Account"
    override val driveConsentCancelled = "Google Drive authorization was cancelled. Please try again."
    override val driveCheckingBackup = "Checking Google Drive for existing backups..."
    override val driveBackupFoundTitle = "Google Drive Backup Found!"
    override val restoreNowAndLogin = "Restore All Data & Sign In"
    override val syncPendingOffline = "Pending sync — will auto-backup when online"
    override val syncingNow = "Syncing to Google Drive..."
    override val syncedJustNow = "Synced with Cloud Vault & Google Drive"
    override val newPhoneRestoreHint = "Sign in with any Gmail on a new phone to restore all records automatically"
    override val driveDisconnectedMsg = "Gmail account disconnected."
    override val dualCloudVaultBadge = "Dual Cloud Vault + Google Drive Active (Zero-403)"
    override val directDriveOAuthSync = "Direct Google Drive OAuth Sync"
    override val saveToDriveAppBtn = "Save Backup File in Google Drive App"
    override val restoreFromDriveAppBtn = "Restore Backup File from Google Drive App"
    override val oauth403GuideTitle = "Error 403 Fix & Any-Gmail (md2005jim@gmail.com) Guide"
    override val oauth403GuideBody = "1. Dual Cloud Vault (Automatic Fix): Connecting any Gmail (such as md2005jim@gmail.com) now authenticates via Google ID Token into your encrypted Cloud Vault automatically — completely bypassing Error 403!\n2. Direct Google Drive App Picker: Use 'Save / Restore Backup File in Google Drive App' above to pick any folder inside your Google Drive app with zero OAuth popup.\n3. Google Cloud Console Tip: To also allow Direct Drive OAuth for all emails without adding test users, open Google Cloud Console -> APIs & Services -> OAuth consent screen and click 'PUBLISH APP'."
    override val exportFormats = "Local Exports & Restore"
    override val exportJson = "JSON Backup"
    override val exportCsv = "CSV Export"
    override val exportExcel = "Excel (.xls)"
    override val exportPdf = "PDF Report"
    override val restoreLocalJson = "Restore JSON"
    override val exportSuccess = "Exported successfully"
    override val settings = "Settings & Security"
    override val themeMode = "Theme"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "System"
    override val currencyLabel = "Currency"
    override val languageLabel = "Language"
    override val securityLock = "Security & App Lock"
    override val pinLock = "Enable PIN Lock"
    override val setPin = "Set / Change PIN"
    override val enter4DigitPin = "Enter 4–6 digit PIN"
    override val biometricUnlock = "Fingerprint / Face Unlock"
    override val biometricNotAvailable = "Biometric hardware unavailable on this device"
    override val notifications = "Notifications Center"
    override val budgetWarningsToggle = "Budget Alerts (80% / 90% / 100%)"
    override val savingsRemindersToggle = "Savings Reminders"
    override val monthlyReportReminderToggle = "Monthly Report Reminders"
    override val billRemindersToggle = "Bill & Recurring Reminders"
    override val markAllRead = "Mark All Read"
    override val changePassword = "Change Password"
    override val currentPassword = "Current Password"
    override val resetAllData = "Reset Account Data"
    override val resetConfirmMsg = "Are you sure you want to permanently clear all records for this account?"
    override val resetDetailsDesc = "All balances, income, expense, transactions, budgets, savings, and notes will reset to 0. Your existing accounts and categories will be kept, and your connected Google Drive backup will be overwritten."
    override val resetVerifyLabel = "Enter Password or App PIN to confirm"
    override val resetVerifyError = "Incorrect password or PIN."
    override val resetSuccessMsg = "All balances and ledger records have been reset to zero."
    override val resetConfirmAction = "Reset Everything"
    override val pinLengthError = "PIN must be 4 to 6 digits."
    override val passwordEmptyError = "Please enter both current and new passwords."
    override val notificationPermissionDenied = "Notification permission was denied in system settings."
    override val offlineFirstBadge = "Offline-First • Auto Drive Backup"
    override val adminDashboard = "Admin Control Panel"
    override val adminOverview = "Overview"
    override val adminUsers = "Users"
    override val adminAllTransactions = "All Transactions"
    override val adminSystemConfig = "System & DB"
    override val totalUsers = "Total Users"
    override val activeUsers = "Active Users"
    override val lockedUsers = "Locked Users"
    override val systemVolume = "System Volume"
    override val addUser = "Add User"
    override val lockUser = "Lock"
    override val unlockUser = "Unlock"
    override val resetUserPassword = "Reset Password"
    override val backupEntireDb = "Backup Entire Database"
    override val restoreEntireDb = "Restore Entire Database"
    override val exportAllData = "Export All Data"
    override val clearAllTrash = "Purge System Trash"
    override val databaseMaintenance = "Database Maintenance"
    override val exitAdmin = "Exit Admin Panel"
    override val catFood = "Food"
    override val catTransport = "Transport"
    override val catShopping = "Shopping"
    override val catMedical = "Medical"
    override val catEducation = "Education"
    override val catFamily = "Family"
    override val catBusiness = "Business"
    override val catSalary = "Salary"
    override val catFreelancing = "Freelancing"
    override val catInvestment = "Investment"
    override val catSavings = "Savings"
    override val catBills = "Bills"
    override val catEntertainment = "Entertainment"
    override val catTravel = "Travel"
    override val catOthers = "Others"
    override val switchProfile = "Switch Profile"
    override val activeProfileLabel = "Active Profile"
    override val createProfile = "New Profile"
    override val editProfile = "Edit Profile"
    override val deleteProfile = "Delete Profile"
    override val profileName = "Profile Name (e.g. Farm, Family, Business)"
    override val profileDesc = "Description (optional)"
    override val separateFinanceHint = "Each profile maintains isolated accounts, categories, transactions, budgets, loans & analytics."
    override val profileCreatedMsg = "New finance profile created"
    override val notesAndTasks = "Keep Notes & Good Tasks"
    override val keepNotesTab = "Keep Notes"
    override val goodTasksTab = "Tasks"
    override val addNote = "New Note"
    override val editNote = "Edit Note"
    override val noteTagsHint = "Tags (comma-separated)"
    override val checklistMode = "Checklist"
    override val addChecklistItem = "+ Add item"
    override val wordCountLabel = "Words"
    override val charCountLabel = "Characters"
    override val pinNote = "Pin"
    override val archiveNote = "Archive"
    override val unarchiveNote = "Unarchive"
    override val activeNotesFilter = "Active"
    override val archivedFilter = "Archive"
    override val trashFilter = "Trash"
    override val addTask = "New Task"
    override val editTask = "Edit Task"
    override val taskTitleLabel = "Task Title"
    override val taskDescLabel = "Details / Notes"
    override val dueDateLabel = "Due Date"
    override val repeatLabel = "Repeat"
    override val subtasksLabel = "Subtasks"
    override val addSubtaskHint = "Add subtask"
    override val pendingTasksFilter = "Pending"
    override val completedTasksFilter = "Completed"
    override val highPriorityFilter = "Urgent / High"
    override val aiAssistantTitle = "AI Assistant"
    override val chatWithAi = "Chat with AI"
    override val configureProviderFirst = "Add Gemini API Key, xKiro, or any Custom AI Provider first to start chatting."
    override val manageProviders = "Providers"
    override val addAiProvider = "Add AI Provider"
    override val providerKindLabel = "Provider Type"
    override val providerNameLabel = "Provider Name"
    override val baseUrlLabel = "Base URL"
    override val apiKeyLabel = "API Key"
    override val modelNameLabel = "Model ID"
    override val supportsVisionLabel = "Supports Image / Vision Input"
    override val askAiHint = "Ask AI anything..."
    override val attachImage = "Image"
    override val clearChat = "Clear"
    override val saveToNote = "Save Note"
    override val savedToNoteMsg = "Saved to Keep Notes"
    override val copiedMsg = "Copied"
    override val smartToolsTitle = "Smart Tools Hub"
    override val catTextTools = "Text Tools"
    override val catFileTools = "File Converters"
    override val catCalcTools = "Calculators"
    override val catImageTools = "Image Tools"
    override val catDailyTools = "Daily Use"
    override val copyText = "Copy"
    override val pasteText = "Paste"
    override val exportTxt = "Save .TXT"
    override val exportPdfBtn = "Save PDF"
    override val pickFileBtn = "Pick File"
    override val pickImageBtn = "Pick Image"
    override val generateBtn = "Generate"
    override val calculateBtn = "Calculate"
    override val allProfilesNetWorthLabel = "All Profiles Net Worth"
    override val askAiCfoLabel = "Ask AI CFO"
    override val searchToolsHint = "Search 38+ smart tools..."
    override val timeframeLifetime = "Lifetime"
    override val timeframeThisYear = "This Year"
    override val timeframeThisMonth = "This Month"
    override val timeframeToday = "Today"
    override val lifetimeSavedHint = "Balances & Lifetime records stay saved permanently until deleted or reset"
    override val gamesArenaTitle = "Play & Earn"
    override val gamesArenaSubtitle = "Dual Wallets • Online Friends • 100% Loss Cashback & Armory"
    override val playGames = "Play & Earn"
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.BANGLA -> BanglaStrings
    AppLanguage.ENGLISH -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { BanglaStrings }
