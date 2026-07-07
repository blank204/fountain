package com.fountain.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Source of truth for which packages are time-gated. Guards against gating our own
 * launcher (spec §2.3: our launcher and system UI must never be gated).
 */
class GatedAppRepository(context: Context) {

    private val dao = FountainDatabase.get(context).gatedAppDao()
    private val ownPackage = context.packageName

    val gatedPackages: Flow<Set<String>> =
        dao.observeGatedPackages().map { it.toSet() }

    suspend fun isGated(packageName: String): Boolean = dao.isGated(packageName)

    suspend fun setGated(packageName: String, gated: Boolean) {
        if (packageName == ownPackage) return
        if (gated) dao.add(GatedApp(packageName)) else dao.remove(packageName)
    }
}
