/*
 * MIT License
 *
 * Copyright (c) 2026 .EXE / Onisong Error
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.onisong.exe.nodes

import java.time.Instant

// ---------------------------------------------------------------------------
// NodeStatus — the outcome produced by any node after evaluation.
// ---------------------------------------------------------------------------

/**
 * Represents the resolved status of a single [SoulNode] evaluation.
 *
 * @property timestamp  UTC instant at which the status was assigned.
 * @property detail     Optional human-readable detail for logging / auditing.
 */
sealed class NodeStatus(
    open val timestamp: Instant = Instant.now(),
    open val detail: String = ""
) {
    /** Node completed its evaluation without issue. */
    data class Pass(
        override val detail: String = "",
        override val timestamp: Instant = Instant.now()
    ) : NodeStatus(timestamp, detail)

    /**
     * Node explicitly blocked execution.
     *
     * @property reason  Machine-readable veto code (e.g. "RESTRICTED_TERM").
     */
    data class Veto(
        val reason: String,
        override val detail: String = "",
        override val timestamp: Instant = Instant.now()
    ) : NodeStatus(timestamp, detail)

    /** Node encountered a recoverable anomaly; execution may continue. */
    data class Warning(
        override val detail: String = "",
        override val timestamp: Instant = Instant.now()
    ) : NodeStatus(timestamp, detail)

    /** Node encountered an unrecoverable error. */
    data class Error(
        val throwable: Throwable? = null,
        override val detail: String = "",
        override val timestamp: Instant = Instant.now()
    ) : NodeStatus(timestamp, detail)

    /** Node has been registered but has not yet been evaluated. */
    object Pending : NodeStatus(detail = "PENDING")
}

// ---------------------------------------------------------------------------
// SoulNode — sealed hierarchy covering all 47 node identities.
// Each subtype carries the data contract relevant to its role.
// ---------------------------------------------------------------------------

/**
 * Sealed class representing every node in the 47-node soul graph.
 *
 * Nodes are grouped into five functional clusters:
 *  - **Perception**   (nodes 01–09)  : raw signal ingestion
 *  - **Analysis**     (nodes 10–19)  : decomposition and classification
 *  - **Validation**   (nodes 20–29)  : constraint enforcement
 *  - **Synthesis**    (nodes 30–38)  : output assembly
 *  - **Governance**   (nodes 39–47)  : audit, memory, lifecycle
 *
 * @property nodeId   Unique numeric identifier (1-based, matches spec).
 * @property label    Human-readable name of the node.
 * @property cluster  Functional cluster this node belongs to.
 * @property status   Current [NodeStatus]; starts as [NodeStatus.Pending].
 */
sealed class SoulNode(
    val nodeId: Int,
    val label: String,
    val cluster: NodeCluster,
    var status: NodeStatus = NodeStatus.Pending
) {

    // -----------------------------------------------------------------------
    // Cluster A — Perception  (nodes 01–09)
    // -----------------------------------------------------------------------

    /** Node 01 — Accepts raw text payloads from external callers. */
    data class InputReceiver(val rawPayload: String = "") :
        SoulNode(1, "InputReceiver", NodeCluster.PERCEPTION)

    /** Node 02 — Tokenises the raw payload into discrete lexical units. */
    data class Tokenizer(val tokens: List<String> = emptyList()) :
        SoulNode(2, "Tokenizer", NodeCluster.PERCEPTION)

    /** Node 03 — Detects the natural language of the input. */
    data class LanguageDetector(val detectedLocale: String = "") :
        SoulNode(3, "LanguageDetector", NodeCluster.PERCEPTION)

    /** Node 04 — Extracts named entities from the token stream. */
    data class EntityExtractor(val entities: Map<String, String> = emptyMap()) :
        SoulNode(4, "EntityExtractor", NodeCluster.PERCEPTION)

    /** Node 05 — Scores sentiment polarity in the range [-1.0, 1.0]. */
    data class SentimentScorer(val score: Float = 0f) :
        SoulNode(5, "SentimentScorer", NodeCluster.PERCEPTION)

    /** Node 06 — Identifies broad intent category of the request. */
    data class IntentClassifier(val intentLabel: String = "") :
        SoulNode(6, "IntentClassifier", NodeCluster.PERCEPTION)

    /** Node 07 — Parses temporal references (dates, durations). */
    data class TemporalParser(val temporalRefs: List<String> = emptyList()) :
        SoulNode(7, "TemporalParser", NodeCluster.PERCEPTION)

    /** Node 08 — Resolves pronoun and co-reference chains. */
    data class CoreferenceResolver(val resolvedRefs: Map<String, String> = emptyMap()) :
        SoulNode(8, "CoreferenceResolver", NodeCluster.PERCEPTION)

    /** Node 09 — Scores ambiguity level in the range [0.0, 1.0]. */
    data class AmbiguityMeter(val ambiguityScore: Float = 0f) :
        SoulNode(9, "AmbiguityMeter", NodeCluster.PERCEPTION)

    // -----------------------------------------------------------------------
    // Cluster B — Analysis  (nodes 10–19)
    // -----------------------------------------------------------------------

    /** Node 10 — Decomposes the request into a dependency parse tree. */
    data class DependencyParser(val parseTree: String = "") :
        SoulNode(10, "DependencyParser", NodeCluster.ANALYSIS)

    /** Node 11 — Scores semantic similarity against a knowledge base. */
    data class SemanticMatcher(val matchScore: Float = 0f) :
        SoulNode(11, "SemanticMatcher", NodeCluster.ANALYSIS)

    /** Node 12 — Classifies request priority on a 0–10 scale. */
    data class PriorityClassifier(val priorityLevel: Int = 0) :
        SoulNode(12, "PriorityClassifier", NodeCluster.ANALYSIS)

    /** Node 13 — Scores the internal coherence of multi-sentence input. */
    data class CoherenceEvaluator(val coherenceScore: Float = 0f) :
        SoulNode(13, "CoherenceEvaluator", NodeCluster.ANALYSIS)

    /** Node 14 — Checks whether the request is inside a known domain. */
    data class DomainBoundaryChecker(val withinDomain: Boolean = false) :
        SoulNode(14, "DomainBoundaryChecker", NodeCluster.ANALYSIS)

    /** Node 15 — Breaks compound requests into atomic sub-tasks. */
    data class TaskDecomposer(val subTasks: List<String> = emptyList()) :
        SoulNode(15, "TaskDecomposer", NodeCluster.ANALYSIS)

    /** Node 16 — Attaches contextual tags based on conversation history. */
    data class ContextTagger(val tags: Set<String> = emptySet()) :
        SoulNode(16, "ContextTagger", NodeCluster.ANALYSIS)

    /** Node 17 — Detects logical contradictions within the request. */
    data class ContradictionDetector(val contradictions: List<String> = emptyList()) :
        SoulNode(17, "ContradictionDetector", NodeCluster.ANALYSIS)

    /** Node 18 — Scores how specific the request is on a 0–100 scale. */
    data class SpecificityScorer(val specificityScore: Int = 0) :
        SoulNode(18, "SpecificityScorer", NodeCluster.ANALYSIS)

    /** Node 19 — Resolves lexical ambiguities using context. */
    data class DisambiguationEngine(val resolvedTerms: Map<String, String> = emptyMap()) :
        SoulNode(19, "DisambiguationEngine", NodeCluster.ANALYSIS)

    // -----------------------------------------------------------------------
    // Cluster C — Validation  (nodes 20–29)
    // -----------------------------------------------------------------------

    /** Node 20 — Checks structural well-formedness of the request. */
    data class SyntaxValidator(val isWellFormed: Boolean = false) :
        SoulNode(20, "SyntaxValidator", NodeCluster.VALIDATION)

    /** Node 21 — Verifies referential integrity within multi-turn context. */
    data class ReferentialIntegrityChecker(val integrityPassed: Boolean = false) :
        SoulNode(21, "ReferentialIntegrityChecker", NodeCluster.VALIDATION)

    /** Node 22 — Evaluates whether input satisfies ethical constraints. */
    data class EthicsFilter(val passed: Boolean = false) :
        SoulNode(22, "EthicsFilter", NodeCluster.VALIDATION)

    /** Node 23 — Enforces rate-limit policy per caller identity. */
    data class RateLimitGuard(val withinLimits: Boolean = false) :
        SoulNode(23, "RateLimitGuard", NodeCluster.VALIDATION)

    /** Node 24 — Validates caller permissions against an access-control list. */
    data class PermissionGate(val permissionsGranted: Set<String> = emptySet()) :
        SoulNode(24, "PermissionGate", NodeCluster.VALIDATION)

    /** Node 25 — Verifies that all declared input types match schema. */
    data class TypeConsistencyChecker(val typesMatch: Boolean = false) :
        SoulNode(25, "TypeConsistencyChecker", NodeCluster.VALIDATION)

    /** Node 26 — Clamps numeric parameters within allowed bounds. */
    data class BoundaryEnforcer(val clamped: Map<String, Number> = emptyMap()) :
        SoulNode(26, "BoundaryEnforcer", NodeCluster.VALIDATION)

    /** Node 27 — Detects duplicate or replayed requests via a signature store. */
    data class DuplicateRequestDetector(val isDuplicate: Boolean = false) :
        SoulNode(27, "DuplicateRequestDetector", NodeCluster.VALIDATION)

    /**
     * Node 28 — **LogicValidator** (primary implementation target).
     *
     * Scans [inputText] for restricted terms. If any are found, the node
     * transitions its [status] to [NodeStatus.Veto] with reason
     * `"RESTRICTED_TERM"`. Otherwise it transitions to [NodeStatus.Pass].
     *
     * @property inputText      The sanitised request string to be scanned.
     * @property detectedTerms  Terms found during the scan (populated post-eval).
     */
    data class LogicValidator(
        val inputText: String = "",
        val detectedTerms: List<String> = emptyList()
    ) : SoulNode(28, "LogicValidator", NodeCluster.VALIDATION)

    /** Node 29 — Cross-checks that multi-step outputs remain mutually consistent. */
    data class CrossReferenceAuditor(val inconsistencies: List<String> = emptyList()) :
        SoulNode(29, "CrossReferenceAuditor", NodeCluster.VALIDATION)

    // -----------------------------------------------------------------------
    // Cluster D — Synthesis  (nodes 30–38)
    // -----------------------------------------------------------------------

    /** Node 30 — Selects the response template appropriate to the intent. */
    data class TemplateSelector(val templateId: String = "") :
        SoulNode(30, "TemplateSelector", NodeCluster.SYNTHESIS)

    /** Node 31 — Assembles a draft response from validated sub-components. */
    data class ResponseAssembler(val draftResponse: String = "") :
        SoulNode(31, "ResponseAssembler", NodeCluster.SYNTHESIS)

    /** Node 32 — Scores response fluency on a 0–1 scale. */
    data class FluencyScorer(val fluencyScore: Float = 0f) :
        SoulNode(32, "FluencyScorer", NodeCluster.SYNTHESIS)

    /** Node 33 — Adapts tone/register to the user's communication profile. */
    data class ToneAdapter(val targetRegister: String = "") :
        SoulNode(33, "ToneAdapter", NodeCluster.SYNTHESIS)

    /** Node 34 — Trims response length to within configured token budget. */
    data class LengthOptimizer(val tokenBudget: Int = 512) :
        SoulNode(34, "LengthOptimizer", NodeCluster.SYNTHESIS)

    /** Node 35 — Injects supporting citations or references as needed. */
    data class CitationInjector(val citations: List<String> = emptyList()) :
        SoulNode(35, "CitationInjector", NodeCluster.SYNTHESIS)

    /** Node 36 — Applies structured formatting (markdown, JSON, etc.). */
    data class FormatterNode(val outputFormat: String = "PLAIN_TEXT") :
        SoulNode(36, "FormatterNode", NodeCluster.SYNTHESIS)

    /** Node 37 — Performs a final readability pass. */
    data class ReadabilityOptimizer(val readabilityScore: Float = 0f) :
        SoulNode(37, "ReadabilityOptimizer", NodeCluster.SYNTHESIS)

    /** Node 38 — Packages the final payload for transmission. */
    data class OutputPackager(val packedPayload: ByteArray = byteArrayOf()) :
        SoulNode(38, "OutputPackager", NodeCluster.SYNTHESIS)

    // -----------------------------------------------------------------------
    // Cluster E — Governance  (nodes 39–47)
    // -----------------------------------------------------------------------

    /** Node 39 — Writes a complete audit entry to persistent storage. */
    data class AuditLogger(val auditEntries: List<String> = emptyList()) :
        SoulNode(39, "AuditLogger", NodeCluster.GOVERNANCE)

    /** Node 40 — Manages rolling and long-term conversational memory. */
    data class MemoryManager(val memorySnapshot: Map<String, Any> = emptyMap()) :
        SoulNode(40, "MemoryManager", NodeCluster.GOVERNANCE)

    /** Node 41 — Records performance telemetry (latency, throughput). */
    data class TelemetryCollector(val metricsMap: Map<String, Long> = emptyMap()) :
        SoulNode(41, "TelemetryCollector", NodeCluster.GOVERNANCE)

    /** Node 42 — Handles structured error escalation and recovery routing. */
    data class ErrorEscalationHandler(val escalatedErrors: List<String> = emptyList()) :
        SoulNode(42, "ErrorEscalationHandler", NodeCluster.GOVERNANCE)

    /** Node 43 — Enforces session lifecycle (creation, expiry, teardown). */
    data class SessionController(val sessionId: String = "", val active: Boolean = false) :
        SoulNode(43, "SessionController", NodeCluster.GOVERNANCE)

    /** Node 44 — Tracks configuration state across hot-reload cycles. */
    data class ConfigWatcher(val configVersion: Int = 0) :
        SoulNode(44, "ConfigWatcher", NodeCluster.GOVERNANCE)

    /** Node 45 — Maintains rolling feedback score from caller-side ACKs. */
    data class FeedbackAggregator(val rollingScore: Float = 0f) :
        SoulNode(45, "FeedbackAggregator", NodeCluster.GOVERNANCE)

    /** Node 46 — Signals health-check status to the system bus. */
    data class HealthMonitor(val healthy: Boolean = true) :
        SoulNode(46, "HealthMonitor", NodeCluster.GOVERNANCE)

    /** Node 47 — Provides a complete checkpoint snapshot for crash recovery. */
    data class CheckpointSaver(val checkpointId: String = "") :
        SoulNode(47, "CheckpointSaver", NodeCluster.GOVERNANCE)
}

// ---------------------------------------------------------------------------
// NodeCluster — enum tagging each node with its functional group.
// ---------------------------------------------------------------------------

/**
 * Functional cluster a [SoulNode] belongs to.
 * Used for routing, filtering, and reporting within [LogicAuditor].
 */
enum class NodeCluster {
    PERCEPTION,
    ANALYSIS,
    VALIDATION,
    SYNTHESIS,
    GOVERNANCE
}
