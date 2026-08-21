# Test Federation Operations Guide

This document describes monitoring responsibilities and incident-response rules for teams working with our federated build.
For an overview of Test Federation concepts and configuration, see the [Test Federation guide](./TEST-FEDERATION.md).

## Monitoring

### Aggregate (master)

https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate

The infrastructure team monitors the health of the 'main' aggregate.

**The monitoring includes:**

- Health of TeamCity agents in use
- Overall duration and status of test buckets
- Global performance issues within our build

**The monitoring excludes:**

- Flaky tests
- Failing tests within domains

### Aggregate (smoke)

https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Aggregate_smoke

The 'smoke' aggregate identifies incidents that prevent all commits from being verified.

Examples of such problems:

- Network issues / dependency resolution issues (e.g., Cache Redirector being down)
- A cross-push that breaks compilation
- A red 'SmokeTest'

This build will be monitored by the infrastructure team.

**The monitoring includes:**
- Health of TeamCity agents
- Overall duration and status of test buckets
- Monitoring of flaky/slow tests
- Monitoring of the status of all tests

### Domain {{name}}
(e.g., JS)
https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_KotlinDev_Domain_Js

Each domain build runs the tests associated with that domain.
The corresponding development team owns the build and is primarily responsible for monitoring it.
Conceptually, these builds are analogous to what the domains' CI builds would be if the domains were maintained in separate repositories.

**The monitoring includes:**
- Monitoring of the overall duration of the build
- Monitoring of test buckets
- Monitoring of the build status
- Monitoring of flaky and slow tests

The infrastructure team provides secondary monitoring.

**The infrastructure team's monitoring includes:**
- Monitoring of the overall health of the TeamCity agents
- Monitoring of the overall resource usage of domains to ensure that it stays within reasonable boundaries

## Incident Guide
### Single Red Domain
If a single domain becomes red or unhealthy (e.g., because it contains failing tests), the corresponding development team is responsible for responding.
While commits to the 'red' domain cannot be verified, commits to other domains can still pass the Test Federation.
Broken domains no longer have to be muted by the infrastructure team.

### Many Red Domains
If multiple domains are 'red', or if a broken build prevents commits to multiple domains from being verified, the priority is elevated.
The infrastructure engineer on duty can assess the situation and may contact the engineers responsible for the 'red' domains.
Muting the corresponding tests is an option if the development teams cannot react quickly. Reverting commits shall
be the last resort.

### Red Smoke Aggregate
If a 'SmokeTest' becomes red, then no further commits can be verified. The priority of the problem is high. The situation shall be
addressed as quickly as possible. Reverting commits is a reasonable option.

### Broken 'Contract' / Missing '@AffectedBy'
If a commit that was safely verified later breaks one or more domains because affected tests were not executed, this indicates a
'test federation misconfiguration'. One domain relied _implicitly_ on the behavior of another domain, and this
behavior was changed. There are several ways to adjust the Test Federation configuration:

**The test is a good 'Contract' test candidate**<br>
If the test is testing exactly this behavior and is therefore a good candidate for a 'Contract' between two domains,
then adding the corresponding `@AffectedBy` annotation is reasonable. "Contracts between domains will reveal themselves over time."

**The test relies only implicitly on the behavior of the other domain**<br>
In this case, it is clear that we do have a 'dependency' on certain behavior, but we do not have a dedicated test to ensure
this behavior. A new test to *explicitly* protect this 'Contract' shall be created and checked in with the corresponding
`@AffectedBy` annotation.

While the monitoring of such situations shall primarily be done by development teams, infrastructure engineers are able to spot
such situations more quickly. In this case, the infrastructure engineer may create an AI-based analysis of the incident and may
also have an AI agent suggest a solution that can be reviewed by the development team. Automated analysis works well in such
cases because the domain that was recently changed is considered healthy. The behavior being relied upon is expected to be more isolated
and easier to fix.
