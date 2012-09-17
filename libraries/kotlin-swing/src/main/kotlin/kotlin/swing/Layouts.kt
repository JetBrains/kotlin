package kotlin.swing

import java.awt.GridBagConstraints

/**
 * Helper function to create a [GridBagConstraints]
 */
fun gridBagContraints(gridx: Int? = null, gridy: Int? = null, fill: Int? = null, anchor: Int? = null,
                      gridwidth: Int? = null, gridheight: Int? = null,
                      weightx: Double? = null, weighty: Double? = null): GridBagConstraints {
    val answer = GridBagConstraints()
    if (gridx != null) {
        answer.gridx = gridx
    }
    if (gridy != null) {
        answer.gridy = gridy
    }
    if (fill != null) {
        answer.fill = fill
    }
    if (anchor != null) {
        answer.anchor = anchor
    }
    if (gridwidth != null) {
        answer.gridwidth = gridwidth
    }
    if (gridheight != null) {
        answer.gridheight = gridheight
    }
    if (weightx != null) {
        answer.weightx = weightx
    }
    if (weighty != null) {
        answer.weighty = weighty
    }
    return answer
}