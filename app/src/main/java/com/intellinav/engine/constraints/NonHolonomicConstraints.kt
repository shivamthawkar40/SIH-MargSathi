package com.intellinav.engine.constraints

import com.intellinav.data.model.VelocityData

class NonHolonomicConstraints {

    private var isNhcEnabled = true

    fun setEnabled(enabled: Boolean) {
        isNhcEnabled = enabled
    }

    fun isEnabled(): Boolean = isNhcEnabled

    /**
     * Applies non-holonomic velocity constraints:
     * Ground vehicles cannot slip sideways or fly upward in standard highway motion.
     * Thus, lateral velocity vy = 0 and vertical velocity vz = 0 in vehicle body frame.
     */
    fun applyConstraints(rawVelocity: VelocityData): VelocityData {
        if (!isNhcEnabled) return rawVelocity

        return rawVelocity.copy(
            lateralSpeedMs = 0.0f, // Zero lateral slip
            verticalSpeedMs = 0.0f  // Zero vertical motion
        )
    }
}
