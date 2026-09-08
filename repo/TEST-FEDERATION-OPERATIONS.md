# Test Federation Operations Guide

This document describes monitoring responsibilities and incident-response rules for teams working with our federated build.
For an overview of Test Federation concepts and configuration, see the [Test Federation guide](./TEST-FEDERATION.md).

## Monitoring

### Aggregate (master)

[Aggregate (master)](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate) combines domain builds.
There is no separate active monitoring of this aggregate by the infrastructure team: domain owners monitor their builds as described below.

The infrastructure team monitors TeamCity agent health, build queues, and resource usage across the build infrastructure,
and responds to infrastructure issues escalated by domain owners or developers.

### Aggregate (smoke)

[Aggregate (smoke)](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate_smoke) detects problems that
block safe-merge for all commits, such as dependency resolution failures, cross-pushes that break compilation, or failing smoke tests.
The infrastructure team monitors this build and leads the response to failures.

**The monitoring includes:**

- TeamCity agent health
- Build and test-bucket duration and status
- All test failures, including flaky and slow tests

### Domain builds

Each domain build, such as [Domain JS](https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Domain_Js),
runs the tests associated with that domain.[^migration] The corresponding development team owns and monitors the build.

**The monitoring includes:**

- Build and test-bucket duration and status
- All build failures, including stable failures and flaky or slow tests

Domain owners should define acceptable build durations for their domains and investigate overruns.
Escalate agent, queue, or resource problems to the infrastructure team, including safe-merge delays not explained by individual domain durations.

## Incident Guide

All investigation coordination and communication must take place in the `#kotlin-build` Slack channel.
Infrastructure duty involvement is not required for domain test failures.
Escalate infrastructure problems to the infrastructure engineer on duty.

### Single Red Domain

The domain owner investigates and resolves the failure. Commits that require the failing tests cannot pass safe-merge until the failure
is resolved or the tests are muted. Commits that do not require those tests can still pass safe-merge.
The infrastructure team does not need to mute the broken domain to unblock unrelated commits.

### Many Red Domains

The affected domain owners coordinate the investigation and response. Prioritize failures that newly block safe-merge across domains.
Multiple red domains alone do not transfer ownership to the infrastructure team; involve infrastructure duty if the problem is
infrastructure-related or also breaks the smoke aggregate.

For domain failures, the responders choose how to restore safe-merge:

1. Identify the scope and contact the relevant owners or change author.
2. Mute failing tests only if it is safe to allow further commits while investigating. Otherwise, keep safe-merge blocked for affected changes.
3. Land a fix promptly if feasible. Otherwise, consider reverting the breaking change, especially if leaving it in place would make recovery harder.

Reverting disrupts the original change, but is appropriate when a timely fix or safe workaround is not available.

### Red Smoke Aggregate

A failing smoke aggregate blocks safe-merge for all commits and requires an urgent response.
The infrastructure engineer on duty leads the response and involves the relevant domain owners or change author for code or test fixes.
Reverting the breaking change is a reasonable option to restore safe-merge quickly.

### Broken 'Contract' / Missing '@AffectedBy'

If a commit passes safe-merge but later breaks another domain because relevant tests were not run, the domain owners investigate
the missing dependency together and adjust the Test Federation configuration:

- If an existing test explicitly checks the required behavior, add the corresponding `@AffectedByXYZ` annotation.
- If the test relies on that behavior only implicitly, create a dedicated contract test with the corresponding annotation.

Both teams must approve the contract; see [Contracts between Domains](./TEST-FEDERATION.md#contracts-between-domains--single-tests--test-suites-affected-by-other-domains).

[^migration]: During migration, some builds contain tests from multiple domains, so a domain aggregate may include tests owned by another domain.
