Run the characterization test suite and report any deviations from pinned behaviour.

Steps:

1. Run the characterization suite:
   ```bash
   ./gradlew test --tests "*.characterization.*" --info
   ```

2. For each failing test, report:
   - Test class and method name
   - What behaviour was pinned (from the assertion message)
   - What the actual behaviour is now
   - Whether this looks like an intentional change or an accidental regression

3. For each passing test, confirm it is still pinning the correct behaviour (spot-check assertion messages match what the code actually does).

4. Summary table:
   | Test | Status | Notes |
   |------|--------|-------|
   | ... | PASS/FAIL | ... |

5. If any test fails: recommend whether to update the pin (behaviour intentionally changed) or fix the regression (behaviour accidentally broken). Do not update pins without explicit instruction.
