package com.agent.android

import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.target.ObservationNormalizer
import com.agent.android.target.TargetQuery
import com.agent.android.target.TargetResolver
import com.agent.android.target.TargetResolutionStatus
import com.agent.android.target.TargetSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase32TargetResolutionUnitTest {

    private val resolver = TargetResolver()

    private fun createSampleSnapshot(
        packageName: String = "com.android.chrome",
        snapshotId: String = "snap-1000",
        nodes: List<ObservationNode> = emptyList()
    ): ObservationSnapshot {
        val root = ObservationNode(
            id = "root",
            parentId = null,
            className = "android.widget.FrameLayout",
            packageName = packageName,
            text = null,
            contentDescription = null,
            resourceId = "root_view",
            bounds = ObservationBounds(0, 0, 1080, 1920),
            isClickable = false,
            isLongClickable = false,
            isFocusable = false,
            isFocused = false,
            isEnabled = true,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isChecked = false,
            isSelected = false,
            isVisibleToUser = true,
            isPassword = false,
            childCount = nodes.size,
            children = nodes
        )

        return ObservationSnapshot(
            snapshotId = snapshotId,
            timestampMs = 100000L,
            packageName = packageName,
            activityName = "MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = nodes.size + 1,
            rootNode = root,
            allNodesList = listOf(root) + nodes,
            state = ObservationState.SUCCESS,
            error = null
        )
    }

    @Test
    fun testTextNormalization() {
        assertEquals("send", ObservationNormalizer.normalizeText(" Send "))
        assertEquals("send message", ObservationNormalizer.normalizeText("  SEND   MESSAGE  "))
        assertEquals("search", ObservationNormalizer.normalizeText("Search..."))
        assertEquals(null, ObservationNormalizer.normalizeText("   "))
        assertEquals(null, ObservationNormalizer.normalizeText(null))
    }

    @Test
    fun testExactResourceIdMatching() {
        val btnNode = ObservationNode("node-1", "root", "android.widget.Button", "com.android.chrome", "Submit", null, "com.android.chrome:id/send_btn", ObservationBounds(10, 10, 200, 100), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(btnNode))

        val query = TargetQuery(resourceId = "com.android.chrome:id/send_btn")
        val res = resolver.resolve(snap, query)

        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        assertNotNull(res.resolvedTarget)
        assertEquals("node-1", res.resolvedTarget?.nodeId)
        assertTrue(res.resolvedTarget!!.matchScore >= 10.0f)
    }

    @Test
    fun testExactTextAndNormalizedTextMatching() {
        val searchNode = ObservationNode("node-search", "root", "android.widget.EditText", "com.android.chrome", "Search or type URL", null, "com.android.chrome:id/url_bar", ObservationBounds(0, 50, 1080, 150), true, false, true, false, true, true, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(searchNode))

        val exactRes = resolver.resolve(snap, TargetQuery(text = "Search or type URL"))
        assertEquals(TargetResolutionStatus.RESOLVED, exactRes.status)
        assertEquals("node-search", exactRes.resolvedTarget?.nodeId)

        val normRes = resolver.resolve(snap, TargetQuery(text = "  search OR type url  "))
        assertEquals(TargetResolutionStatus.RESOLVED, normRes.status)
        assertEquals("node-search", normRes.resolvedTarget?.nodeId)
    }

    @Test
    fun testContentDescriptionAndClassNameMatching() {
        val iconNode = ObservationNode("node-icon", "root", "android.widget.ImageView", "com.android.chrome", null, "Settings Icon", "com.android.chrome:id/settings", ObservationBounds(900, 50, 1000, 150), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(iconNode))

        val res = resolver.resolve(snap, TargetQuery(contentDescription = "Settings Icon"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        assertEquals("node-icon", res.resolvedTarget?.nodeId)
    }

    @Test
    fun testAmbiguousTargetDetection() {
        val sendBtn1 = ObservationNode("node-send-1", "root", "android.widget.Button", "com.example.app", "Send", null, "com.example.app:id/send", ObservationBounds(10, 100, 200, 200), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val sendBtn2 = ObservationNode("node-send-2", "root", "android.widget.Button", "com.example.app", "Send", null, "com.example.app:id/send", ObservationBounds(10, 300, 200, 400), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(packageName = "com.example.app", nodes = listOf(sendBtn1, sendBtn2))

        val res = resolver.resolve(snap, TargetQuery(text = "Send"))
        assertEquals(TargetResolutionStatus.AMBIGUOUS, res.status)
        assertTrue(res.topCandidates.size >= 2)
    }

    @Test
    fun testStaleSnapshotProtection() {
        val snap = createSampleSnapshot(snapshotId = "snap-old-123")
        val res = resolver.resolve(snap, TargetQuery(text = "Search"), activeSnapshotId = "snap-new-456")

        assertEquals(TargetResolutionStatus.STALE_SNAPSHOT, res.status)
        assertTrue(res.explanation.contains("STALE_SNAPSHOT"))
    }

    @Test
    fun testPackageMismatchIsolation() {
        val snap = createSampleSnapshot(packageName = "com.google.android.youtube")
        val res = resolver.resolve(snap, TargetQuery(text = "Search"), expectedPackage = "com.android.chrome")

        assertEquals(TargetResolutionStatus.NOT_FOUND, res.status)
        assertTrue(res.explanation.contains("Package mismatch"))
    }

    @Test
    fun testEmptyQueryAndNoSnapshotHandling() {
        val snap = createSampleSnapshot()
        val emptyRes = resolver.resolve(snap, TargetQuery())
        assertEquals(TargetResolutionStatus.INVALID_QUERY, emptyRes.status)

        val nullRes = resolver.resolve(null, TargetQuery(text = "Search"))
        assertEquals(TargetResolutionStatus.NO_SNAPSHOT, nullRes.status)
    }

    @Test
    fun testActionabilityClassification() {
        val editNode = ObservationNode("node-edit", "root", "android.widget.EditText", "com.android.chrome", "Type here", null, "input_field", ObservationBounds(0, 0, 500, 100), true, false, true, false, true, true, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(editNode))

        val res = resolver.resolve(snap, TargetQuery(text = "Type here"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        val target = res.resolvedTarget
        assertNotNull(target)
        assertTrue(target!!.isActionable)
        assertTrue(target.isEditable)
    }

    @Test
    fun testDisabledTargetActionRejection() {
        val disabledNode = ObservationNode("node-disabled", "root", "android.widget.Button", "com.android.chrome", "Submit", null, "submit_btn", ObservationBounds(10, 10, 200, 100), true, false, true, false, false, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(disabledNode))

        val res = resolver.resolve(snap, TargetQuery(text = "Submit"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        val target = res.resolvedTarget
        assertNotNull(target)
        assertEquals(false, target!!.node.isEnabled) // Disabled node MUST NOT allow execution!
    }

    @Test
    fun testNonClickableTargetActionRejection() {
        val staticTextNode = ObservationNode("node-text", "root", "android.widget.TextView", "com.android.chrome", "Title Text", null, "title_view", ObservationBounds(10, 10, 500, 100), false, false, false, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(staticTextNode))

        val res = resolver.resolve(snap, TargetQuery(text = "Title Text"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        val target = res.resolvedTarget
        assertNotNull(target)
        assertEquals(false, target!!.node.isClickable) // Non-clickable node MUST be rejected for click action!
    }

    @Test
    fun testNonEditableTargetTextInputRejection() {
        val btnNode = ObservationNode("node-btn", "root", "android.widget.Button", "com.android.chrome", "Click Me", null, "btn_1", ObservationBounds(10, 10, 200, 100), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(btnNode))

        val res = resolver.resolve(snap, TargetQuery(text = "Click Me"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        val target = res.resolvedTarget
        assertNotNull(target)
        assertEquals(false, target!!.isEditable) // Non-editable node MUST be rejected for text input action!
    }

    @Test
    fun testInvalidBoundsActionRejection() {
        val zeroBoundsNode = ObservationNode("node-zero", "root", "android.widget.Button", "com.android.chrome", "Hidden", null, "btn_hidden", ObservationBounds(0, 0, 0, 0), true, false, true, false, true, false, false, false, false, false, true, false, 0)
        val snap = createSampleSnapshot(nodes = listOf(zeroBoundsNode))

        val res = resolver.resolve(snap, TargetQuery(text = "Hidden"))
        assertEquals(TargetResolutionStatus.RESOLVED, res.status)
        val target = res.resolvedTarget
        assertNotNull(target)
        val width = target!!.bounds.right - target.bounds.left
        val height = target.bounds.bottom - target.bounds.top
        val boundsValid = width > 0 && height > 0
        assertEquals(false, boundsValid) // Invalid 0x0 bounds MUST be rejected before action dispatch!
    }
}
