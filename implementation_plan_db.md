Implementation Plan
37 minutes ago

Review
Client-Side Enhancements & Databinding Improvements
Goal Description
There are two primary goals for this phase of work:

Module Renaming: Rename all pure client-side modules to use the verrai-client-* prefix to clearly distinguish them from any future server-side or shared components.
DataBinding Completeness: Build a demonstration page in the verrai-app sample to highlight current databinding features and visually identify gaps. Then, implement framework fixes in the processor to resolve those gaps (e.g., real-time input syncing and nested property observability).
User Review Required
WARNING

Renaming modules involves changing directory names,
pom.xml
configurations, and potentially IDE settings. This is a large structural change. Please review the "Module Renaming" section to confirm the list of modules to be renamed is correct.

Proposed Changes
1. Module Renaming (verrai-client-*)
   We will rename the following modules (both directory names and artifactIds in
   pom.xml
   ):

verrai-api -> verrai-client-api
verrai-security -> verrai-client-security
verrai-ui -> verrai-client-ui
verrai-widgets-bootstrap -> verrai-client-widgets-bootstrap
verrai-widgets-material -> verrai-client-widgets-material
verrai-persistence -> verrai-client-persistence
verrai-processor -> verrai-client-processor
verrai-annotation-processor -> verrai-client-annotation-processor
verrai-app -> verrai-client-app (Optional: or keep it as the sample container)
We will update the root
pom.xml
and all inter-module dependencies to reflect these new artifactIds.

2. Sample Application (Demonstration Phase)
   [NEW] DataBindingDemoPage.java
   Create a new page class annotated with @Page(role = "databinding") and @Templated.
   Inject a @Model annotated POJO (e.g., DemoModel) that contains simple properties (String name) and nested properties (e.g., Address address).
   Bind these properties using @Bound to various
   DataField
   inputs to demonstrate two-way binding.
   [NEW] DataBindingDemoPage.html
   Create the HTML template for the page.
   Add text inputs and spans bound to the model properties.
   [NEW] DemoModel.java & Address.java
   Create simple @Bindable POJOs to establish an object graph to test nested property observability.
   [MODIFY] DashboardPage.html &
   DashboardPage.java
   Add a navigation button linking to the new DataBindingDemoPage so it can be easily accessed.
3. Framework Fixes (DataBinding Implementation Phase)
   Once the demo page is up, we expect to observe the following issues which we will fix in the newly renamed verrai-client-processor module:

Real-time Input Syncing: Two-way binding currently attaches to the "change" event for HTML elements. Text inputs should ideally bind to "input" so that the model updates on every keystroke, not just on blur.
Nested Property Observation: Currently, @Bound(property = "user.name") only attaches a property change listener to the root model object, listening for user changes. It does not listen to the inner
User
proxy for name changes. We need to implement chained property listeners in
TemplatedProcessor
.
Verification Plan
Automated Tests
Run mvn clean install to ensure annotation processors successfully generate the _Binder classes for the new demo components and that all renamed modules compile correctly.
Make sure existing processor tests pass.
Manual Verification
Deploy the teaVM application locally (or check generated JS).
Navigate to the DataBinding Demo Page.
Type in the input fields and verify that the bound spans update in real-time.
Update nested properties and verify the UI reflects those deep changes automatically.