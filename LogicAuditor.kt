/*
 * MIT License
 *
 * Copyright (c) 2026 Project.EXE / Onisong Error
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

package com.onisong.error.hemisphereB

import com.onisong.error.core.SystemBridge
import com.onisong.error.nodes.NodeStatus
import com.onisong.error.nodes.SoulNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.logging.Logger

// ---------------------------------------------------------------------------
// Domain models
// ---------------------------------------------------------------------------

/**
 * Immutable snapshot of the auditor's state during a single Thought Loop cycle.
 *
 * @property round          Current round index (1-based, max [LogicAuditor.TOTAL_ROUNDS]).
 * @property phase          Active [ThoughtPhase] for this round.
 * @property activeNodeId   ID of the [SoulNode] being evaluated this round.
 * @property lastStatus     Outcome of the most recently completed node evaluation.
 * @property vetoed         True once a [NodeStatus.Veto] has been emitted; loop halts.
 * @property timestamp      UTC instant this snapshot was produced.
 */
data class ThoughtLoopState(
    val round: Int,
    val phase: ThoughtPhase,
    val activeNodeId: Int,
    val lastStatus: NodeStatus = NodeStatus.Pending,
    val vetoed: Boolean = false,
    val timestamp: Instant = Instant.now()
)

/**
 * The 7 discrete phases that map one-to-one with the 7 rounds of the Thought Loop.
 *
 * Ordinal is used as the round index (0-based internally; add 1 for display).
 */
enum class ThoughtPhase {
    /** Round 1 — Ingest the raw request from Hemisphere A. */
    INGEST,

    /** Round 2 — Classify intent and cluster the request. */
    CLASSIFY,

    /** Round 3 — Run restricted-term scan (Node 28 — LogicValidator). */
    VALIDATE,

    /** Round 4 — Perform cross-reference and consistency audit. */
    AUDIT,

    /** Round 5 — Synthesise a preliminary response frame. */
    SYNTHESISE,

    /** Round 6 — Apply governance checks (permissions, rate-limits). */
    GOVERN,

    /** Round 7 — Commit the result or propagate a veto upstream. */
    COMMIT
}

/**
 * Represents the final, resolved output of a complete Thought Loop run.
 *
 * @property accepted       True if all rounds passed without a veto.
 * @property finalStatus    Consolidated [NodeStatus] from the terminal round.
 * @property auditTrail     Ordered list of [ThoughtLoopState] snapshots (one per round).
 * @property processingMs   Total wall-clock duration of the loop in milliseconds.
 */
data class AuditResult(
    val accepted: Boolean,
    val finalStatus: NodeStatus,
    val auditTrail: List<ThoughtLoopState>,
    val processingMs: Long
)

// ---------------------------------------------------------------------------
// LogicAuditor — Hemisphere B
// ---------------------------------------------------------------------------

/**
 * **LogicAuditor** is the primary reasoning auditor for Hemisphere B.
 *
 * Responsibilities:
 * - Implement [SystemBridge] so it can be registered on the central interface bus.
 * - Drive a 7-round Thought Loop via a cold [Flow] of [ThoughtLoopState] snapshots.
 * - Delegate round 3 (VALIDATE) to the [Node28LogicValidator] strategy.
 * - Expose [auditRequest] as the single public entry point for Hemisphere A callers.
 *
 * **Thread safety:** Each call to [auditRequest] creates an independent [Flow];
 * instances of this class are safe to share across coroutines provided no mutable
 * external state is injected.
 *
 * @property config  Runtime configuration for this auditor instance.
 */
class LogicAuditor(
    private val config: AuditorConfig = AuditorConfig()
) : SystemBridge {

    // -----------------------------------------------------------------------
    // SystemBridge identity
    // -----------------------------------------------------------------------

    override val bridgeId: String = "HEMISPHERE_B_LOGIC_AUDITOR"
    override val version: String = "1.0.0"

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        /** Fixed number of rounds in one complete Thought Loop. */
        const val TOTAL_ROUNDS: Int = 7

        private val log: Logger = Logger.getLogger(LogicAuditor::class.java.name)
    }

    // -----------------------------------------------------------------------
    // Node strategies
    // -----------------------------------------------------------------------

    /** Delegated strategy for round 3 — Node 28 restricted-term validation. */
    private val node28Validator = Node28LogicValidator(config.restrictedTerms)

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Audits [request] from Hemisphere A through the full 7-round Thought Loop.
     *
     * The returned [Flow] emits exactly one [ThoughtLoopState] per round (7 total),
     * then completes. If any round yields a [NodeStatus.Veto], subsequent rounds are
     * skipped and a terminal veto state is emitted instead.
     *
     * Callers should collect the flow and inspect [ThoughtLoopState.vetoed] or
     * await [collectAuditResult] for a consolidated [AuditResult].
     *
     * @param request   Raw request string forwarded from Hemisphere A.
     * @return          Cold [Flow] of [ThoughtLoopState]; one emission per round.
     */
    fun auditRequest(request: String): Flow<ThoughtLoopState> = flow {
        log.info("[$bridgeId] Starting Thought Loop for request (len=${request.length})")

        val trail = mutableListOf<ThoughtLoopState>()
        var currentVetoed = false
        var lastStatus: NodeStatus = NodeStatus.Pending

        for (phase in ThoughtPhase.entries) {
            val round = phase.ordinal + 1 // 1-based display

            if (currentVetoed) {
                // Emit a skipped-state to preserve the 7-emission contract.
                val skipped = ThoughtLoopState(
                    round = round,
                    phase = phase,
                    activeNodeId = resolveNodeIdForPhase(phase),
                    lastStatus = lastStatus,
                    vetoed = true
                )
                trail.add(skipped)
                emit(skipped)
                continue
            }

            val nodeId = resolveNodeIdForPhase(phase)
            log.fine("[$bridgeId] Round $round / $TOTAL_ROUNDS — phase=$phase nodeId=$nodeId")

            // Evaluate the node assigned to this phase.
            val status: NodeStatus = evaluatePhase(phase, request)

            currentVetoed = status is NodeStatus.Veto
            lastStatus = status

            val snapshot = ThoughtLoopState(
                round = round,
                phase = phase,
                activeNodeId = nodeId,
                lastStatus = status,
                vetoed = currentVetoed
            )

            trail.add(snapshot)
            emit(snapshot)

            if (currentVetoed) {
                log.warning(
                    "[$bridgeId] VETO issued at round $round (phase=$phase). " +
                    "Reason: ${(status as NodeStatus.Veto).reason}"
                )
            }
        }

        log.info(
            "[$bridgeId] Thought Loop complete. " +
            "Vetoed=$currentVetoed Rounds=${trail.size}"
        )
    }

    /**
     * Convenience terminal operator that collects [auditRequest] into a single
     * [AuditResult].
     *
     * Must be called from within a coroutine scope.
     *
     * @param request  Raw request string forwarded from Hemisphere A.
     * @return         Fully resolved [AuditResult].
     */
    suspend fun collectAuditResult(request: String): AuditResult {
        val start = System.currentTimeMillis()
        val trail = mutableListOf<ThoughtLoopState>()

        auditRequest(request).collect { state ->
            trail.add(state)
        }

        val terminal = trail.lastOrNull()
        val vetoed = terminal?.vetoed ?: false
        val finalStatus = terminal?.lastStatus ?: NodeStatus.Error(
            detail = "Thought Loop produced no states."
        )

        return AuditResult(
            accepted = !vetoed,
            finalStatus = finalStatus,
            auditTrail = trail.toList(),
            processingMs = System.currentTimeMillis() - start
        )
    }

    // -----------------------------------------------------------------------
    // SystemBridge overrides
    // -----------------------------------------------------------------------

    /**
     * Performs a lightweight liveness check for the system bus.
     *
     * @return `true` if this bridge is operational.
     */
    override fun isAlive(): Boolean = true

    /**
     * Returns a structured map of diagnostic metadata for this bridge.
     */
    override fun diagnostics(): Map<String, Any> = mapOf(
        "bridgeId"         to bridgeId,
        "version"          to version,
        "totalRounds"      to TOTAL_ROUNDS,
        "restrictedTerms"  to config.restrictedTerms.toList(),
        "uptimeSinceMs"    to System.currentTimeMillis()
    )

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Routes evaluation to the appropriate node handler for [phase].
     *
     * Each phase maps to a specific [SoulNode] cluster and evaluation strategy.
     * Unimplemented phases return [NodeStatus.Pass] as a safe default; replace
     * each stub with a concrete handler as the node graph is built out.
     *
     * @param phase    Active [ThoughtPhase].
     * @param request  Raw request string available to all evaluators.
     * @return         Resolved [NodeStatus] for this phase.
     */
    private fun evaluatePhase(phase: ThoughtPhase, request: String): NodeStatus {
        return when (phase) {
            ThoughtPhase.INGEST     -> evaluateIngest(request)
            ThoughtPhase.CLASSIFY   -> evaluateClassify(request)
            ThoughtPhase.VALIDATE   -> node28Validator.evaluate(request) // Node 28
            ThoughtPhase.AUDIT      -> evaluateAudit(request)
            ThoughtPhase.SYNTHESISE -> evaluateSynthesise(request)
            ThoughtPhase.GOVERN     -> evaluateGovern(request)
            ThoughtPhase.COMMIT     -> evaluateCommit(request)
        }
    }

    /**
     * Returns the canonical [SoulNode] ID assigned to [phase].
     * Round-to-node mapping follows the specification sheet.
     */
    private fun resolveNodeIdForPhase(phase: ThoughtPhase): Int = when (phase) {
        ThoughtPhase.INGEST     -> 1   // InputReceiver
        ThoughtPhase.CLASSIFY   -> 12  // PriorityClassifier
        ThoughtPhase.VALIDATE   -> 28  // LogicValidator  ← primary target
        ThoughtPhase.AUDIT      -> 29  // CrossReferenceAuditor
        ThoughtPhase.SYNTHESISE -> 31  // ResponseAssembler
        ThoughtPhase.GOVERN     -> 24  // PermissionGate
        ThoughtPhase.COMMIT     -> 39  // AuditLogger
    }

    // -- Phase stubs (replace with concrete node implementations) ------------

    private fun evaluateIngest(request: String): NodeStatus {
        if (request.isBlank()) {
            return NodeStatus.Error(detail = "Request payload is blank.")
        }
        return NodeStatus.Pass(detail = "Payload ingested (len=${request.length})")
    }

    private fun evaluateClassify(@Suppress("UNUSED_PARAMETER") request: String): NodeStatus =
        NodeStatus.Pass(detail = "Classification stub — pass-through.")

    private fun evaluateAudit(@Suppress("UNUSED_PARAMETER") request: String): NodeStatus =
        NodeStatus.Pass(detail = "Cross-reference audit stub — pass-through.")

    private fun evaluateSynthesise(@Suppress("UNUSED_PARAMETER") request: String): NodeStatus =
        NodeStatus.Pass(detail = "Synthesis stub — pass-through.")

    private fun evaluateGovern(@Suppress("UNUSED_PARAMETER") request: String): NodeStatus =
        NodeStatus.Pass(detail = "Governance stub — pass-through.")

    private fun evaluateCommit(@Suppress("UNUSED_PARAMETER") request: String): NodeStatus =
        NodeStatus.Pass(detail = "Commit stub — pass-through.")
}

// ---------------------------------------------------------------------------
// Node28LogicValidator — isolated evaluation strategy
// ---------------------------------------------------------------------------

/**
 * Encapsulates the evaluation logic for **Node 28 — LogicValidator**.
 *
 * Scanning is performed case-insensitively. A match on any term in
 * [restrictedTerms] immediately produces a [NodeStatus.Veto]; the scan
 * continues after the first hit only to collect all matching terms for
 * the audit trail.
 *
 * @property restrictedTerms  Set of lower-case terms that trigger a veto.
 */
internal class Node28LogicValidator(
    private val restrictedTerms: Set<String>
) {

    /**
     * Evaluates [input] against the restricted-term list.
     *
     * @param input  Sanitised request string from the Thought Loop.
     * @return       [NodeStatus.Veto] if any restricted term is found;
     *               [NodeStatus.Pass] otherwise.
     */
    fun evaluate(input: String): NodeStatus {
        val lower = input.lowercase()
        val hits = restrictedTerms.filter { term -> lower.contains(term) }

        return if (hits.isNotEmpty()) {
            NodeStatus.Veto(
                reason = "RESTRICTED_TERM",
                detail  = "Detected restricted term(s): ${hits.joinToString()}"
            )
        } else {
            NodeStatus.Pass(detail = "Node 28 — no restricted terms detected.")
        }
    }
}

// ---------------------------------------------------------------------------
// AuditorConfig — runtime configuration value object
// ---------------------------------------------------------------------------

/**
 * Immutable configuration for a [LogicAuditor] instance.
 *
 * @property restrictedTerms  Lower-case terms whose presence causes Node 28 to veto.
 *                            Callers should normalise terms to lower-case before passing.
 */
data class AuditorConfig(
    val restrictedTerms: Set<String> = setOf(
        "delete",
        "system",
        "root",
        "sudo",
        "exec",
        "shell",
        "format",
        "wipe",
        "override",
        "bypass"
    )
)
