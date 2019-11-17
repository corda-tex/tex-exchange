package com.ccc

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity

val ALICE = TestIdentity(CordaX500Name(organisation = "Alice", locality = "CordaCodeClubLand", country = "US"))
val BOB = TestIdentity(CordaX500Name(organisation = "Bob", locality = "CordaCodeClubCity", country = "US"))
