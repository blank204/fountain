# ProGuard/R8 rules for Fountain.
#
# Currently unused: the release build sets isMinifyEnabled = false.
# This file exists because app/build.gradle.kts references it in proguardFiles,
# and enabling minification without it would fail the build.
#
# Room, Compose, and DataStore all ship their own consumer rules, so no manual
# keep rules are needed yet. Add them here if minification is enabled and
# reflection-dependent code breaks.
