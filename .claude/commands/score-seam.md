Score the extraction risk for a named seam using the agentic-scouts subagent pattern.

Seam to score: $ARGUMENTS

Steps:

1. **Gather explicit context** for the seam — do not rely on the subagent to explore the codebase itself:
   - Read all Java files in the package matching `$ARGUMENTS`
   - Build a dependency list: what does this seam import, what imports it
   - Note test coverage: does `src/test/` have any tests touching these classes?
   - Note any shared DB tables or raw JDBC calls

2. **Spawn a Task subagent** with the following prompt (fill in the gathered context inline — the subagent has no access to the filesystem):

   ```
   You are a seam risk scorer. Score the following seam for microservice extraction risk.
   Return a JSON object only — no explanation, no markdown.

   Seam: $ARGUMENTS
   Classes: [list from step 1]
   Dependencies: [list from step 1]
   Test coverage: [from step 1]
   Shared DB / raw JDBC: [from step 1]

   Scoring rubric:
   - coupling: 1 (low) to 5 (high) — how many other packages depend on this seam
   - test_coverage_pct: 0–100 — estimated % of behaviour covered by existing tests
   - data_model_tangle: 1 (clean) to 5 (deeply tangled) — how much the data model is shared with other seams
   - business_criticality: "H", "M", or "L"
   - overall_extraction_risk: "low", "medium", or "high"
   - confidence: 0.0–1.0
   - rationale: one sentence

   Return format:
   {"seam":"...","coupling":N,"test_coverage_pct":N,"data_model_tangle":N,"business_criticality":"...","overall_extraction_risk":"...","confidence":N,"rationale":"..."}
   ```

3. **Display the verdict** and compare it against the human ranking in `docs/adr/001-service-decomposition.md`:
   - Agreement or disagreement on overall risk level
   - If disagreement: which specific score drives the difference
