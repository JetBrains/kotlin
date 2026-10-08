# Test Federation Operations Guide

This guide defines who monitors which builds, and the fixed workflow to follow when a build turns red.
For Test Federation concepts and configuration, see the [Test Federation guide](./TEST-FEDERATION.md).

## Table of contents

- [Ownership and channels](#ownership-and-channels)
- [Monitoring setup](#monitoring-setup)
- [Response workflow](#response-workflow)
- [Message templates](#message-templates)
- [Playbooks](#playbooks)

## Ownership and channels

| Build                                                                                                          | Owner                                | Automated notifications                 |
|----------------------------------------------------------------------------------------------------------------|--------------------------------------|-----------------------------------------|
| [Aggregate (smoke)](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate_smoke) | Infrastructure engineer on duty      | `#kotlin-bots`                          |
| Domain builds, e.g. [Domain (Js)](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Domain_Js) | Domain team[^migration]              | `#kotlin-bots` and the team's channel   |
| [Aggregate (master)](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate)      | Nobody directly: it combines the domain builds | `#kotlin-bots`                |
| TeamCity agents, queues, resources                                                                             | Infrastructure team                  | Internal infrastructure monitoring      |

| Channel            | Use for                                                                                  |
|--------------------|------------------------------------------------------------------------------------------|
| `#kotlin-bots`     | Automated TeamCity notifications only. Do not discuss incidents here.                   |
| `#kotlin-build`    | **All incident communication**: one thread per incident, using the [templates](#message-templates). |
| Team channel       | Automated notifications for the team's domain build and internal team discussions.      |

Reach the infrastructure engineer on duty with `@kotlin-infrastructure-duty` in `#kotlin-build`.

## Monitoring setup

Each domain team must have:

1. **Slack notifications for its domain build in the team's channel.** Configure them in the build DSL
   (`kotlin-teamcity-build`, `.teamcity/common/buildtypes/domains.kt`), as `CoreLibs` and `Native` already do:

   ```kotlin
   features {
       slackNotifications(JetbrainsSlackChannel.KotlinLibTeam) {
           firstFailureAfterSuccess = true
           newBuildProblemOccurred = true
           firstSuccessAfterFailure = true
       }
   }
   ```

   Add the team's channel to `JetbrainsSlackChannel` in `.teamcity/common/notifications.kt` if it does not exist yet.
2. **A named monitor** for each week, who reacts to these notifications: stable failures, flaky tests, and slow tests.
   This person owns the [response workflow](#response-workflow) for the domain.
3. **A target build duration.** The monitor checks it weekly and opens an issue for the team if the build is consistently slower.

Domain teams do not monitor agents, queues, or resource usage. If a failure is not explained by your domain's changes or tests,
[escalate](#escalation-message) without diagnosing the root cause first.

## Response workflow

Follow these steps for every red build that you own:

| Step           | What to do                                                                                                      | When                                                |
|----------------|-----------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| 1. Acknowledge | Start a thread in `#kotlin-build` with the [incident message](#incident-message). Assign the TeamCity investigation to yourself. | Within 1 working hour (smoke: immediately)          |
| 2. Triage      | Find the breaking change (TeamCity *Changes* tab, *Build Log*, test history). Mention the change author in the thread. If it is not a domain problem, [escalate](#escalation-message). | Right after acknowledging |
| 3. Restore     | Pick the first option that applies: **mute** if the failure is test-only and safe to ignore temporarily, **fix** if a fix can land within a few hours, **revert** otherwise. Post an [update](#update-message). | Same working day (smoke: as fast as possible) |
| 4. Close       | When the build is green, post the [resolution](#resolved-message) and resolve the TeamCity investigation.       | When green                                          |

Rules:

- One incident, one thread. Keep all updates in the thread, not in DMs.
- Every mute needs a YouTrack issue that is assigned to the test owner and linked in the TeamCity mute.
- A revert does not need the author's approval if the author does not respond within 1 working hour.

## Message templates

Copy these into `#kotlin-build`.

### Incident message

```
🔴 <Domain name | Smoke> is red: <TeamCity build link>
Failure: <failing test or build problem>
Blocks safe-merge for: <changes in <domain> | all commits>
Suspected change: <commit/MR link | unknown>
Owner: @<you>
Next step: <investigating | muting | fixing | reverting>
```

### Update message

```
Update: <mute | fix | revert> in progress: <MR/commit/YouTrack link>. ETA: <time>.
```

### Resolved message

```
✅ Resolved by <mute | fix | revert>: <link>. Follow-up: <YouTrack issue | none>
```

### Escalation message

```
@kotlin-infrastructure-duty <TeamCity build link> fails for a reason that is not caused by the domain.
Symptom: <agent failure | dependency resolution | timeout | stuck in queue | other>
Blocks: <changes in <domain> | all commits>
```

## Playbooks

### Single red domain

- **Lead:** the domain monitor.
- **Impact:** only commits that require the failing tests are blocked. Unrelated commits still pass safe-merge, so infra does not need to mute anything.
- **Action:** follow the [response workflow](#response-workflow).

### Many red domains

- **Lead:** the monitor of the domain that owns the breaking change. If that is unclear, the first monitor to acknowledge leads.
- **Action:** post **one** incident thread. The other monitors reply in that thread instead of starting new ones.
- Prioritize failures that block safe-merge across domains. Reverting is preferred when many domains are affected.
- Many red domains do not make it an infra incident. Escalate only if the cause is infrastructure or Aggregate (smoke) is red too.

### Red smoke aggregate

- **Lead:** the infrastructure engineer on duty. **Impact:** safe-merge is blocked for **all** commits.
- **Action:** follow the [response workflow](#response-workflow) immediately. Mention the relevant domain monitor and change author in the thread.
- Mute the test if it is safe. Otherwise, revert the breaking change. Do not wait for a fix.

### Infrastructure problem

- **Symptoms:** agent failures, dependency resolution errors, timeouts, stuck queues, or failures that cannot be reproduced locally or linked to a change.
- **Action:** post the [escalation message](#escalation-message) in the incident thread. The infrastructure engineer on duty takes over the lead.
  The domain monitor stays in the thread to confirm when the domain build is green again.

### Broken contract / missing `@MustRunOnChangesInXYZ`

A commit passed safe-merge but broke another domain because the relevant tests were not required.

1. Restore the build with the [response workflow](#response-workflow).
2. The monitor of the broken domain opens a YouTrack issue for the missing test requirement and assigns it to both teams.
3. Adjust the Test Federation configuration:
   - If an existing test checks the behavior explicitly, add `@MustRunOnChangesInXYZ`, where `XYZ` is the domain whose changes must run the test.
   - If the behavior is only covered implicitly, add a dedicated contract test with that annotation.
4. Both teams approve the change. See [Running individual tests on changes in another domain](./TEST-FEDERATION.md#running-individual-tests-on-changes-in-another-domain)
   and [Extra: Contract tests](./TEST-FEDERATION.md#extra-contract-tests).

[^migration]: During migration, some builds contain tests from multiple domains, so a domain build may include tests owned by another domain.
