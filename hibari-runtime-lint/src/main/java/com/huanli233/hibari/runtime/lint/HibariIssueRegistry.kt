package com.huanli233.hibari.runtime.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class HibariIssueRegistry: IssueRegistry() {

    override val issues: List<Issue> get() = listOf(
        TunableNamingDetector.ISSUE
    )

    override val minApi = 14
    override val api = CURRENT_API
    override val vendor = Vendor(
        vendorName = "Hibari",
        identifier = "com.huanli233.hibari.runtime",
        feedbackUrl = "https://github.com/huanli233/Hibari/issues"
    )

}